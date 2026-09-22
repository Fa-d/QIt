package dev.sadakat.qit.presentation.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.model.Ayah
import dev.sadakat.qit.core.domain.model.BanglaVoice
import dev.sadakat.qit.core.domain.model.ListeningProgress
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.model.SurahListening
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.player.WordPointer
import dev.sadakat.qit.core.domain.repository.ListeningHistory
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.domain.repository.QuranText
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import dev.sadakat.qit.core.domain.repository.WordMeanings
import dev.sadakat.qit.core.domain.repository.stateOf
import dev.sadakat.qit.watch.WatchConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot result of a "send to watch" request, shown as a snackbar. */
sealed interface ReaderMessage {
    data class SentToWatch(val watches: Int) : ReaderMessage
    data object NoWatch : ReaderMessage
}

data class SurahReaderUiState(
    val surah: Surah? = null,
    val ayahs: List<Ayah> = emptyList(),
    val loadFailed: Boolean = false,
    val mode: RecitationMode = RecitationMode.ARABIC_BANGLA,
    val downloadState: SurahDownloadState = SurahDownloadState.NotDownloaded,
    /** The ayah of this surah that is currently playing, if any. */
    val playingAyah: Int? = null,
    /** Ayah to scroll to when the reader opens; 0 = start from the top. */
    val initialAyah: Int = 0,
    /** Reading comfort prefs: whether translations are drawn and the list mirrors the recitation. */
    val showTranslation: Boolean = true,
    val followAlong: Boolean = true,
    val message: ReaderMessage? = null,
    /** Times each ayah was heard, by ayah - 1; empty until known. */
    val heard: List<Int> = emptyList(),
    /** How far this surah has been listened to; null while nothing of it has been heard. */
    val listening: SurahListening? = null,
    /** Each word's meaning, by ayah, in the word-by-word language; empty while word by word is off. */
    val wordMeanings: Map<Int, List<String>> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest: drop the meanings of a language switched away from.
@HiltViewModel
@Suppress("LongParameterList") // One surah brings its text, settings, audio, playback, watch and listening together.
class SurahReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    quranText: QuranText,
    private val settings: QuranSettings,
    private val downloads: SurahDownloads,
    private val player: QuranPlayer,
    private val watch: WatchConnection,
    history: ListeningHistory,
    wordMeanings: WordMeanings,
) : ViewModel() {

    private val surahNumber: Int = savedStateHandle.get<Int>("surah") ?: 1
    private val initialAyah: Int = savedStateHandle.get<Int>("ayah") ?: 0

    private data class ReaderLoad(
        val surah: Surah? = null,
        val ayahs: List<Ayah> = emptyList(),
        val failed: Boolean = false,
    )

    private val load = flow {
        val surah = quranText.surah(surahNumber)
        emit(ReaderLoad(surah = surah, ayahs = quranText.ayahs(surahNumber)))
    }.catch { emit(ReaderLoad(failed = true)) }

    private val message = MutableStateFlow<ReaderMessage?>(null)

    private var currentMode: RecitationMode = RecitationMode.ARABIC_BANGLA
    private var currentVoice: BanglaVoice = BanglaVoice.DEFAULT

    init {
        viewModelScope.launch { settings.mode.collect { currentMode = it } }
        viewModelScope.launch { settings.banglaVoice.collect { currentVoice = it } }
    }

    private data class Heard(val perAyah: List<Int>, val listening: SurahListening?)

    /** This surah's listening, per ayah and as a whole. */
    private val heard = history.counts.map { counts ->
        val listening = ListeningProgress.surah(counts, surahNumber)
        Heard(
            (1..QuranMeta.ayahCount(surahNumber)).map {
                counts.count(surahNumber, it)
            },
            listening.takeIf { it.isHeard },
        )
    }

    /** This surah's word meanings in the word-by-word language; empty while it is off or they fail to load. */
    private val meanings = settings.readingPrefs
        .map { it.wordByWord }
        .distinctUntilChanged()
        .flatMapLatest { language ->
            flow { emit(wordMeanings.meanings(surahNumber, language)) }.catch { emit(emptyMap()) }
        }

    private data class Reading(
        val mode: RecitationMode,
        val voice: BanglaVoice,
        val prefs: ReadingPrefs,
        val meanings: Map<Int, List<String>>,
    )

    val uiState: StateFlow<SurahReaderUiState> = combine(
        load,
        combine(settings.mode, settings.banglaVoice, settings.readingPrefs, meanings, ::Reading),
        combine(downloads.states, heard) { states, heard -> states to heard },
        player.nowPlaying,
        message,
    ) { load, reading, (states, heard), nowPlaying, message ->
        SurahReaderUiState(
            surah = load.surah,
            ayahs = load.ayahs,
            loadFailed = load.failed,
            mode = reading.mode,
            downloadState = states.stateOf(surahNumber, reading.mode.tracks(reading.voice)),
            playingAyah = nowPlaying?.takeIf { it.surah == surahNumber }?.ayah,
            initialAyah = initialAyah,
            showTranslation = reading.prefs.showTranslation,
            followAlong = reading.prefs.followAlong,
            message = message,
            heard = heard.perAyah,
            listening = heard.listening,
            wordMeanings = reading.meanings,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SurahReaderUiState(initialAyah = initialAyah),
    )

    /** The word pointer over this surah's reciting ayah; Off while another surah (or nothing) plays. */
    val pointer: StateFlow<WordPointer> = combine(player.nowPlaying, player.pointer) { nowPlaying, pointer ->
        if (nowPlaying?.surah == surahNumber) pointer else WordPointer.Off
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordPointer.Off)

    fun playAyah(ayah: Int) {
        player.play(surahNumber, ayah, currentMode)
    }

    /** Plays from the basmala when the surah has one, else from verse 1. */
    fun playSurah() {
        val fromAyah = if (QuranMeta.hasBasmalaPrefix(surahNumber)) 0 else 1
        player.play(surahNumber, fromAyah, currentMode)
    }

    fun download() {
        downloads.download(surahNumber, currentMode.tracks(currentVoice))
    }

    fun remove() {
        downloads.remove(surahNumber, currentMode.tracks(currentVoice))
    }

    /** Persists [mode]; if this surah is playing, restarts it at the current ayah in the new mode. */
    fun setMode(mode: RecitationMode) {
        viewModelScope.launch {
            settings.setMode(mode)
            player.nowPlaying.value?.let { current ->
                if (current.surah == surahNumber) player.play(surahNumber, current.ayah, mode)
            }
        }
    }

    fun sendToWatch() {
        viewModelScope.launch {
            message.value = if (!watch.isWatchReachable()) {
                ReaderMessage.NoWatch
            } else {
                watch.sendDownload(surahNumber, currentMode.tracks(currentVoice)).fold(
                    onSuccess = { ReaderMessage.SentToWatch(it) },
                    onFailure = { ReaderMessage.NoWatch },
                )
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}
