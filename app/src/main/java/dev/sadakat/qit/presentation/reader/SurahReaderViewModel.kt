package dev.sadakat.qit.presentation.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.model.Ayah
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.domain.repository.QuranText
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import dev.sadakat.qit.core.domain.repository.stateOf
import dev.sadakat.qit.watch.WatchConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
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
)

@HiltViewModel
class SurahReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    quranText: QuranText,
    private val settings: QuranSettings,
    private val downloads: SurahDownloads,
    private val player: QuranPlayer,
    private val watch: WatchConnection,
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

    init {
        viewModelScope.launch { settings.mode.collect { currentMode = it } }
    }

    val uiState: StateFlow<SurahReaderUiState> = combine(
        load,
        combine(settings.mode, settings.readingPrefs) { mode, prefs -> mode to prefs },
        downloads.states,
        player.nowPlaying,
        message,
    ) { load, (mode, prefs), states, nowPlaying, message ->
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
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SurahReaderUiState(initialAyah = initialAyah),
    )

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
