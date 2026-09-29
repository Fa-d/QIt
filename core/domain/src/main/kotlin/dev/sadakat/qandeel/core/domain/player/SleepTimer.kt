package dev.sadakat.qandeel.core.domain.player

import kotlin.math.PI
import kotlin.math.cos

/**
 * A running sleep timer: [option], started at [startedAtMs] on a monotonic clock. Pure: the player
 * adapter asks it, as time passes, what to show, how loud to play and when to stop.
 *
 * The last [FADE_MS] fade the volume out along an equal-power curve (it sounds even, where a linear
 * fade seems to drop off at the end), so a listener falling asleep isn't woken by a sudden stop.
 */
data class SleepTimer(val option: SleepOption, val startedAtMs: Long) {

    /** What the UI shows at [nowMs]; [remainingSurahMs] is the playing time left in the surah, if known. */
    fun status(nowMs: Long, remainingSurahMs: Long? = null, speed: Float = 1f): SleepTimerStatus {
        val remaining = remainingMs(nowMs, remainingSurahMs, speed) ?: return SleepTimerStatus.EndOfSurah
        return when {
            remaining > FADE_MS -> when (option) {
                is SleepOption.Minutes -> SleepTimerStatus.Counting(remaining)
                SleepOption.EndOfSurah -> SleepTimerStatus.EndOfSurah
            }

            else -> SleepTimerStatus.FadingOut(remaining.coerceAtLeast(0))
        }
    }

    /** The playback volume (0..1) at [nowMs]. */
    fun volume(nowMs: Long, remainingSurahMs: Long? = null, speed: Float = 1f): Float {
        val remaining = remainingMs(nowMs, remainingSurahMs, speed) ?: return 1f
        if (remaining >= FADE_MS) return 1f
        val faded = 1 - remaining.coerceAtLeast(0).toDouble() / FADE_MS
        return cos(faded * PI / 2).toFloat()
    }

    /** Whether a timed stop is due; the end-of-surah stop happens when the surah ends. */
    fun isDue(nowMs: Long): Boolean = option is SleepOption.Minutes && nowMs >= endMs(option)

    /**
     * Wall-clock time until the stop, or null while it can't be known (end of surah before the
     * last ayah's duration is loaded). Media time runs [speed] times faster than the wall clock.
     */
    private fun remainingMs(nowMs: Long, remainingSurahMs: Long?, speed: Float): Long? = when (option) {
        is SleepOption.Minutes -> endMs(option) - nowMs
        SleepOption.EndOfSurah -> remainingSurahMs?.let { (it / speed).toLong() }
    }

    private fun endMs(option: SleepOption.Minutes) = startedAtMs + option.minutes * MS_PER_MINUTE

    companion object {
        /** How long the fade-out lasts before the stop. */
        const val FADE_MS = 20_000L
        private const val MS_PER_MINUTE = 60_000L
    }
}
