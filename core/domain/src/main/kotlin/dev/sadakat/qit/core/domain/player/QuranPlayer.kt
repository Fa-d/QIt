package dev.sadakat.qit.core.domain.player

import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

data class NowPlaying(
    val surah: Int,
    /** 0 while the basmala before verse 1 plays. */
    val ayah: Int,
    val track: Track,
    val mode: RecitationMode,
    val isPlaying: Boolean,
    val isBuffering: Boolean
)

/** Plays a surah ayah by ayah. Implemented in :core:data on the app-wide ExoPlayer. */
interface QuranPlayer {

    /** Null when nothing is queued. */
    val nowPlaying: StateFlow<NowPlaying?>

    /** Last playback error (no network for an undownloaded surah, ...). Cleared when playback resumes. */
    val error: StateFlow<String?>

    /** Replaces the queue with [surah] in [mode] and starts at [fromAyah] (0 = basmala). */
    fun play(surah: Int, fromAyah: Int = 1, mode: RecitationMode)

    fun togglePlayPause()

    /** Jumps to the first item of the next ayah (stops at the end of the surah). */
    fun nextAyah()

    /** Restarts the current ayah if it has played for more than 3 s, else jumps to the previous ayah. */
    fun previousAyah()

    fun stop()

    /** Queues the last saved position, paused unless [playWhenReady]. No-op if nothing is saved or something is queued. */
    fun restoreLast(playWhenReady: Boolean = false)
}
