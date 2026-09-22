package dev.sadakat.qit.core.domain.player

import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import kotlinx.coroutines.flow.Flow
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

/**
 * Where playback is: [itemPositionMs] into the current item (what the word pointer follows) and
 * [surahPositionMs] of [surahDurationMs] into the whole surah (0 while the lengths are unknown).
 */
data class PlaybackProgress(val itemPositionMs: Long, val surahPositionMs: Long, val surahDurationMs: Long) {
    /** How far through the surah, 0..1 (0 while its length is unknown). */
    val fraction: Float get() = if (surahDurationMs <=
        0
    ) {
        0f
    } else {
        (surahPositionMs.toFloat() / surahDurationMs).coerceIn(0f, 1f)
    }

    /** Time left in the surah. */
    val remainingMs: Long get() = (surahDurationMs - surahPositionMs).coerceAtLeast(0)

    companion object {
        val START = PlaybackProgress(0, 0, 0)
    }
}

/** Why playback stopped. */
enum class PlaybackError {
    /** Streaming failed: no connection, or the server couldn't be reached. Downloading the surah avoids it. */
    NETWORK,

    /** Anything else: a file that couldn't be read or decoded. */
    FAILED,
}

/** Plays a surah ayah by ayah. Implemented in :core:data on the app-wide ExoPlayer. */
@Suppress("TooManyFunctions") // The player's whole surface: queue, transport, seeks, repeat, speed, sleep and errors.
interface QuranPlayer {

    /** Null when nothing is queued. */
    val nowPlaying: StateFlow<NowPlaying?>

    /** Last playback error; cleared when playback resumes. */
    val error: StateFlow<PlaybackError?>

    /** Replaces the queue with [surah] in [mode] and starts at [fromAyah] (0 = basmala). */
    fun play(surah: Int, fromAyah: Int = 1, mode: RecitationMode)

    /** After an [error], prepares the queue again and plays from where it stopped; a no-op otherwise. */
    fun retry()

    /**
     * Plays from word [word] (0-based, as in `ArabicWords.ranges`) of [ayah] of [surah] in [mode].
     * If that surah is already queued in that mode it seeks in place, without rebuilding the queue;
     * otherwise it queues the surah from that ayah first. Falls back to the ayah's start when the
     * word timings aren't known.
     */
    fun playFromWord(surah: Int, ayah: Int, word: Int, mode: RecitationMode)

    /**
     * Plays [ayah] of [surah] in [mode] on repeat, [times] times in all (null = until changed).
     * Atomic: the queue and the repeat are set together, so the repeat can't be lost to the reset
     * that [play] does for a new surah.
     */
    fun repeatAyah(surah: Int, ayah: Int, mode: RecitationMode, times: Int? = null)

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
     * Where playback is, sampled often while playing (for the word pointer and the surah time bar)
     * and once whenever it pauses or moves. A separate flow from [nowPlaying] for the same reason as
     * [sleepTimer]; collect it only while it's shown.
     */
    val progress: Flow<PlaybackProgress>

    /** The word being recited in the playing ayah; changes once per word. */
    val pointer: Flow<WordPointer>

    /** Moves to [surahPositionMs] into the queued surah, as if it were one recording. */
    fun seekTo(surahPositionMs: Long)

    /**
     * The sleep timer. A separate flow from [nowPlaying] because it ticks every second while counting,
     * and most of the UI doesn't care.
     */
    val sleepTimer: StateFlow<SleepTimerStatus>

    /** Repeats an ayah or a range of the queued surah; lasts until changed or a new surah is played. */
    fun setRepeat(repeat: RepeatSetting)

    /** Changes the recitation speed; the choice is remembered for later sessions. */
    fun setSpeed(speed: PlaybackSpeed)

    /**
     * Stops playback after [option], fading the volume out over the last seconds; replaces a running
     * timer. Null cancels the running timer.
     */
    fun setSleepTimer(option: SleepOption?)
}
