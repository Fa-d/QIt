package dev.sadakat.qit.presentation.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.model.ArabicWords
import dev.sadakat.qit.core.domain.model.Ayah
import dev.sadakat.qit.core.domain.model.ListeningProgress
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.model.SurahListening
import dev.sadakat.qit.core.domain.model.WordByWord
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot result of a "send to watch" request, shown as a snackbar. */
sealed interface ReaderMessage {
    data class SentToWatch(val watches: Int) : ReaderMessage
    data object NoWatch : ReaderMessage
}

/** One word of the glossary with its meaning, or null when none is known for it. */
data class GlossaryWord(val arabic: String, val meaning: String?)

/** The long-pressed ayah: its text, its translation in the current mode, and each word with its meaning. */
data class AyahActionsUi(
    val ayah: Int,
    val arabic: String,
    /** The mode's translation, or English for Arabic only; null if the ayah has none. */
    val translation: String?,
    val words: List<GlossaryWord>,
)

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
    /** The long-pressed ayah's actions (its words with their meanings); null while they are closed. */
    val ayahActions: AyahActionsUi? = null,
)

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest: drop the meanings of a language switched away from.
@HiltViewModel
// One surah brings its text, settings, audio, playback, watch and listening together, and its
// reader its actions: splitting the ViewModel up would only hide that.
@Suppress("LongParameterList", "TooManyFunctions")
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

    /** Bumped by [retry] to restart the load. */
    private val retries = MutableStateFlow(0)

    private val load = retries.flatMapLatest { retry ->
        flow {
            if (retry > 0) emit(ReaderLoad()) // a retry goes back to loading first
            val surah = quranText.surah(surahNumber)
            emit(ReaderLoad(surah = surah, ayahs = quranText.ayahs(surahNumber)))
        }.catch { emit(ReaderLoad(failed = true)) }
    }

    private val message = MutableStateFlow<ReaderMessage?>(null)

    /** The long-pressed ayah whose actions are open; null while they are closed. */
    private val openAyah = MutableStateFlow<Int?>(null)

    private var currentMode: RecitationMode = RecitationMode.ARABIC_BANGLA

    init {
        viewModelScope.launch { settings.mode.collect { currentMode = it } }
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

    private data class Reading(val mode: RecitationMode, val prefs: ReadingPrefs, val meanings: Map<Int, List<String>>)

    /** The glossary's language: the word-by-word setting's, or English while word by word is off. */
    private val glossaryLanguage = settings.readingPrefs
        .map { prefs -> prefs.wordByWord }
        .distinctUntilChanged()
        .map { wordByWord -> wordByWord.takeIf { it != WordByWord.OFF } ?: WordByWord.ENGLISH }

    /** The open ayah's word meanings, requested only while the actions are open. */
    private val glossaryMeanings = combine(openAyah, glossaryLanguage, ::Pair)
        .flatMapLatest { (ayah, language) ->
            if (ayah == null) {
                flowOf(emptyMap())
            } else {
                flow { emit(wordMeanings.meanings(surahNumber, language)) }.catch { emit(emptyMap()) }
            }
        }

    private data class Actions(val openAyah: Int?, val mode: RecitationMode, val meanings: Map<Int, List<String>>)

    private val actions = combine(openAyah, settings.mode, glossaryMeanings, ::Actions)

    val uiState: StateFlow<SurahReaderUiState> = combine(
        load,
        combine(settings.mode, settings.readingPrefs, meanings, ::Reading),
        combine(downloads.states, heard) { states, heard -> states to heard },
        combine(player.nowPlaying, message, actions) { nowPlaying, message, actions ->
            Triple(nowPlaying, message, actions)
        },
    ) { load, (mode, prefs, meanings), (states, heard), (nowPlaying, message, actions) ->
        SurahReaderUiState(
            surah = load.surah,
            ayahs = load.ayahs,
            loadFailed = load.failed,
            mode = mode,
            downloadState = states.stateOf(surahNumber, mode.tracks),
            playingAyah = nowPlaying?.takeIf { it.surah == surahNumber }?.ayah,
            initialAyah = initialAyah,
            showTranslation = prefs.showTranslation,
            followAlong = prefs.followAlong,
            message = message,
            heard = heard.perAyah,
            listening = heard.listening,
            wordMeanings = meanings,
            ayahActions = actionsOf(load, actions),
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

    /** The long-pressed ayah with each word and its meaning; null while closed or unknown. */
    private fun actionsOf(load: ReaderLoad, actions: Actions): AyahActionsUi? {
        val number = actions.openAyah ?: return null
        val ayah = load.ayahs.getOrNull(number - 1) ?: return null
        val meanings = actions.meanings[number].orEmpty()
        return AyahActionsUi(
            ayah = number,
            arabic = ayah.arabic,
            translation = actions.mode.translation?.let { ayah.translation(it) } ?: ayah.english,
            words = ArabicWords.ranges(ayah.arabic).mapIndexed { index, range ->
                GlossaryWord(ayah.arabic.substring(range), meanings.getOrNull(index))
            },
        )
    }

    fun playAyah(ayah: Int) {
        player.play(surahNumber, ayah, currentMode)
    }

    /** Plays from the basmala when the surah has one, else from verse 1. */
    fun playSurah() {
        val fromAyah = if (QuranMeta.hasBasmalaPrefix(surahNumber)) 0 else 1
        player.play(surahNumber, fromAyah, currentMode)
    }

    fun download() {
        downloads.download(surahNumber, currentMode.tracks)
    }

    fun remove() {
        downloads.remove(surahNumber, currentMode.tracks)
    }

    /** Plays from word [word] (0-based) of [ayah] in the current mode. */
    fun playFromWord(ayah: Int, word: Int) {
        player.playFromWord(surahNumber, ayah, word, currentMode)
    }

    /** Repeats [ayah] in the current mode until changed. */
    fun repeatAyah(ayah: Int) {
        player.repeatAyah(surahNumber, ayah, currentMode, times = null)
    }

    /** Loads the surah again after a failure. */
    fun retry() {
        retries.value++
    }

    /** Opens the long-pressed [ayah]'s actions, with its words' meanings. */
    fun showAyahActions(ayah: Int) {
        openAyah.value = ayah
    }

    fun dismissAyahActions() {
        openAyah.value = null
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
                watch.sendDownload(surahNumber, currentMode.tracks).fold(
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
