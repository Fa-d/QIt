package dev.sadakat.qit.wear.presentation.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.PlaybackError
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.QuranText
import dev.sadakat.qit.wear.audio.StreamVolume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** What is queued, the current ayah's text, and everything the controls page shows. */
data class WearNowPlayingUiState(
    val surahNumber: Int? = null,
    val surahName: String? = null,
    val ayah: Int = 0,
    val ayahText: String? = null,
    /** The mode's translation of the current ayah; null in Arabic-only mode and for the basmala. */
    val translation: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val error: PlaybackError? = null,
    /** How far through the surah playback is, 0..1 (by ayah; the basmala is 0). */
    val progress: Float = 0f,
    /** Media volume as a 0..1 fraction, turned by the crown. */
    val volume: Float = 1f,
)

/** Now playing: the ayah's Arabic and translation, the transport controls, and the crown volume. */
@OptIn(ExperimentalCoroutinesApi::class) // mapLatest: cancel a stale ayah-text lookup.
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val quranText: QuranText,
    private val player: QuranPlayer,
    private val streamVolume: StreamVolume,
) : ViewModel() {

    val uiState: StateFlow<WearNowPlayingUiState> = combine(
        player.nowPlaying.mapLatest(::toUiState),
        player.error,
        streamVolume.level,
    ) { state, error, volume -> state.copy(error = error, volume = volume) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearNowPlayingUiState())

    fun previousAyah() = player.previousAyah()

    fun togglePlayPause() = player.togglePlayPause()

    fun nextAyah() = player.nextAyah()

    fun adjustVolume(steps: Int) = streamVolume.adjust(steps)

    /** Ayah 0 is the basmala prefix, which has no entry in [QuranText.ayahs]; the screen shows the basmala itself. */
    private suspend fun toUiState(nowPlaying: NowPlaying?): WearNowPlayingUiState {
        if (nowPlaying == null) return WearNowPlayingUiState()
        val surahName = runCatching { quranText.surah(nowPlaying.surah).nameEnglish }.getOrNull()
        val ayah = if (nowPlaying.ayah >= 1) {
            runCatching { quranText.ayahs(nowPlaying.surah).getOrNull(nowPlaying.ayah - 1) }.getOrNull()
        } else {
            null
        }
        return WearNowPlayingUiState(
            surahNumber = nowPlaying.surah,
            surahName = surahName,
            ayah = nowPlaying.ayah,
            ayahText = ayah?.arabic,
            translation = nowPlaying.mode.translation?.let { track -> ayah?.translation(track) },
            isPlaying = nowPlaying.isPlaying,
            isBuffering = nowPlaying.isBuffering,
            progress = nowPlaying.progress,
        )
    }
}
