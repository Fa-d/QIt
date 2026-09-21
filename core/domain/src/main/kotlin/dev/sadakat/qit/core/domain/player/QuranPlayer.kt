package dev.sadakat.qit.core.domain.player

import dev.sadakat.qit.core.domain.model.QuranMeta
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
    val isBuffering: Boolean,
    val speed: PlaybackSpeed = PlaybackSpeed.X1,
    val repeat: RepeatSetting = RepeatSetting.Off,
) {
    /** Ayahs in the playing surah. */
    val ayahCount: Int get() = QuranMeta.ayahCount(surah)

    /** How far through the surah playback is, 0..1 (by ayah; the basmala is 0). */
    val progress: Float get() = ayah.toFloat() / ayahCount
}

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

    /**
     * Queues the last saved position, paused unless [playWhenReady].
     * No-op if nothing is saved or something is already queued.
     */
    fun restoreLast(playWhenReady: Boolean = false)

    /**
     * The sleep timer. A separate flow from [nowPlaying] because it ticks every second while counting,
     * and most of the UI doesn't care.
     */
    val sleepTimer: StateFlow<SleepTimerStatus>

    /** Repeats an ayah or a range of the queued surah; lasts until changed or a new surah is played. */
    fun setRepeat(repeat: RepeatSetting)

    /** Changes the recitation speed; the choice is remembered for later sessions. */
    fun setSpeed(speed: PlaybackSpeed)

    /** Stops playback after [option], fading the volume out over the last seconds. Replaces a running timer. */
    fun startSleepTimer(option: SleepOption)

    fun cancelSleepTimer()
}
