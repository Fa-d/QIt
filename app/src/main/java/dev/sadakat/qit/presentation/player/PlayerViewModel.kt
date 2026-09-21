package dev.sadakat.qit.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.QuranText
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class PlayerBarUiState(
    val nowPlaying: NowPlaying? = null,
    /** English name of the playing surah; null while unknown. */
    val surahName: String? = null,
    val error: String? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val player: QuranPlayer,
    quranText: QuranText,
) : ViewModel() {

    init {
        // Rebuild the queue from the last session, paused, so the player bar reappears where
        // playback left off (a no-op when something is already queued).
        player.restoreLast(playWhenReady = false)
    }

    private val surahNames = flow {
        emit(quranText.surahs().associate { it.number to it.nameEnglish })
    }.catch {
        // Names are cosmetic; the bar falls back to "Surah N".
        emit(emptyMap())
    }

    // The last error the user dismissed; suppresses it until a new one arrives.
    private val dismissedError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PlayerBarUiState> = combine(
        player.nowPlaying,
        surahNames,
        player.error,
        dismissedError,
    ) { nowPlaying, names, error, dismissed ->
        PlayerBarUiState(
            nowPlaying = nowPlaying,
            surahName = nowPlaying?.let { names[it.surah] },
            error = error?.takeIf { it != dismissed },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerBarUiState())

    fun togglePlayPause() = player.togglePlayPause()

    fun nextAyah() = player.nextAyah()

    fun previousAyah() = player.previousAyah()

    fun stop() = player.stop()

    fun consumeError() {
        dismissedError.value = player.error.value
    }
}
