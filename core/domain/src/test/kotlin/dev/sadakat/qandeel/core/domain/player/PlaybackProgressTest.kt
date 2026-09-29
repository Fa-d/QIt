package dev.sadakat.qandeel.core.domain.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackProgressTest {

    @Test
    fun `fraction and time left of the surah`() {
        val progress = PlaybackProgress(itemPositionMs = 1_000, surahPositionMs = 30_000, surahDurationMs = 120_000)
        assertEquals(0.25f, progress.fraction)
        assertEquals(90_000L, progress.remainingMs)
    }

    @Test
    fun `an unknown length is no progress`() {
        assertEquals(0f, PlaybackProgress.START.fraction)
        assertEquals(0L, PlaybackProgress.START.remainingMs)
    }

    @Test
    fun `positions past the end are clamped`() {
        val past = PlaybackProgress(itemPositionMs = 0, surahPositionMs = 130_000, surahDurationMs = 120_000)
        assertEquals(1f, past.fraction)
        assertEquals(0L, past.remainingMs)
    }
}
