package dev.sadakat.qandeel.core.domain.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {

    private val start = 1_000_000L
    private val tenMinutes = SleepTimer(SleepOption.Minutes(10), startedAtMs = start)
    private val end = start + 600_000L

    @Test
    fun `counts down until the fade`() {
        assertEquals(SleepTimerStatus.Counting(600_000L), tenMinutes.status(start))
        assertEquals(SleepTimerStatus.Counting(30_000L), tenMinutes.status(end - 30_000L))
        assertEquals(1f, tenMinutes.volume(end - 30_000L))
    }

    @Test
    fun `fades out over the last twenty seconds`() {
        assertEquals(SleepTimerStatus.FadingOut(10_000L), tenMinutes.status(end - 10_000L))
        assertEquals(1f, tenMinutes.volume(end - SleepTimer.FADE_MS), 0.001f)
        // Equal power: half-way through the fade the volume is cos(45°), not 0.5.
        assertEquals(0.7071f, tenMinutes.volume(end - SleepTimer.FADE_MS / 2), 0.001f)
        assertEquals(0f, tenMinutes.volume(end), 0.001f)
    }

    @Test
    fun `is due exactly at the end and stays due`() {
        assertFalse(tenMinutes.isDue(end - 1))
        assertTrue(tenMinutes.isDue(end))
        assertTrue(tenMinutes.isDue(end + 5_000))
        assertEquals(SleepTimerStatus.FadingOut(0L), tenMinutes.status(end + 5_000))
    }

    @Test
    fun `end of surah waits for the surah to end instead of the clock`() {
        val endOfSurah = SleepTimer(SleepOption.EndOfSurah, startedAtMs = start)

        assertEquals(SleepTimerStatus.EndOfSurah, endOfSurah.status(start + 3_600_000L))
        assertFalse(endOfSurah.isDue(start + 3_600_000L))
        assertEquals(1f, endOfSurah.volume(start))
    }

    @Test
    fun `end of surah fades over the last seconds of recitation, in wall time`() {
        val endOfSurah = SleepTimer(SleepOption.EndOfSurah, startedAtMs = start)

        assertEquals(SleepTimerStatus.EndOfSurah, endOfSurah.status(start, remainingSurahMs = 45_000L))
        // 15 s of audio at 1.5x lasts 10 s.
        assertEquals(
            SleepTimerStatus.FadingOut(10_000L),
            endOfSurah.status(start, remainingSurahMs = 15_000L, speed = 1.5f),
        )
        assertEquals(0.7071f, endOfSurah.volume(start, remainingSurahMs = 15_000L, speed = 1.5f), 0.001f)
    }
}
