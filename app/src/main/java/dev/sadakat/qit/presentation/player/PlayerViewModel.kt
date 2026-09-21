package dev.sadakat.qit.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.QuranText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerBarUiState(
    val nowPlaying: NowPlaying? = null,
    /** English name of the playing surah; null while unknown. */
    val surahName: String? = null,
    val error: String? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(private val player: QuranPlayer, quranText: QuranText) : ViewModel() {

    // The error the user dismissed; hidden until the player clears it (a retry) or reports another.
    private val dismissedError = MutableStateFlow<String?>(null)

    init {
        // Rebuild the queue from the last session, paused, so the player bar reappears where
        // playback left off (a no-op when something is already queued).
        player.restoreLast(playWhenReady = false)
        // The player clears its error when playback is retried; forget the dismissal then, so the
        // same failure happening again is shown again (the messages are fixed strings).
        viewModelScope.launch { player.error.collect { if (it == null) dismissedError.value = null } }
    }

    private val surahNames = flow {
        emit(quranText.surahs().associate { it.number to it.nameEnglish })
    }.catch {
        // Names are cosmetic; the bar falls back to "Surah N".
        emit(emptyMap())
    }

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
