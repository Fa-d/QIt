package dev.sadakat.qit.wear.presentation.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.QuranText
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Now playing: what is queued, its Arabic text, and the playback controls. */
@HiltViewModel
class NowPlayingViewModel @Inject constructor(private val quranText: QuranText, private val player: QuranPlayer) :
    ViewModel() {

    data class UiState(
        val surahNumber: Int? = null,
        val surahName: String? = null,
        val ayah: Int = 0,
        val ayahText: String? = null,
        val isPlaying: Boolean = false,
        val isBuffering: Boolean = false,
        val error: String? = null,
    )

    val uiState: StateFlow<UiState> = combine(
        player.nowPlaying.mapLatest(::toUiState),
        player.error,
    ) { state, error -> state.copy(error = error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun previousAyah() = player.previousAyah()

    fun togglePlayPause() = player.togglePlayPause()

    fun nextAyah() = player.nextAyah()

    /** Ayah 0 is the basmala prefix, which has no entry in [QuranText.ayahs]; the screen shows the basmala itself. */
    private suspend fun toUiState(nowPlaying: NowPlaying?): UiState {
        if (nowPlaying == null) return UiState()
        val surahName = runCatching { quranText.surah(nowPlaying.surah).nameEnglish }.getOrNull()
        val ayahText = if (nowPlaying.ayah >= 1) {
            runCatching { quranText.ayahs(nowPlaying.surah).getOrNull(nowPlaying.ayah - 1)?.arabic }.getOrNull()
        } else {
            null
        }
        return UiState(
            surahNumber = nowPlaying.surah,
            surahName = surahName,
            ayah = nowPlaying.ayah,
            ayahText = ayahText,
            isPlaying = nowPlaying.isPlaying,
            isBuffering = nowPlaying.isBuffering,
        )
    }
}
