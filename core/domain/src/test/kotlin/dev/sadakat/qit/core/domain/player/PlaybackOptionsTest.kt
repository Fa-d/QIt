package dev.sadakat.qit.core.domain.player

import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaybackOptionsTest {

    @Test
    fun `repeats need at least two plays`() {
        assertThrows(IllegalArgumentException::class.java) { RepeatSetting.Ayah(times = 1) }
        assertThrows(IllegalArgumentException::class.java) { RepeatSetting.Range(1, 3, times = 1) }
        RepeatSetting.Ayah(times = 2)
        RepeatSetting.Ayah(times = null)
    }

    @Test
    fun `a range runs forward from its first ayah`() {
        assertThrows(IllegalArgumentException::class.java) { RepeatSetting.Range(from = 5, to = 4, times = null) }
        assertThrows(IllegalArgumentException::class.java) { RepeatSetting.Range(from = 0, to = 4, times = null) }
        RepeatSetting.Range(from = 4, to = 4, times = null)
    }

    @Test
    fun `a sleep timer needs a positive duration`() {
        assertThrows(IllegalArgumentException::class.java) { SleepOption.Minutes(0) }
        SleepOption.Minutes(1)
    }

    @Test
    fun `now playing reports its place in the surah`() {
        val nowPlaying =
            NowPlaying(2, 143, Track.ARABIC, RecitationMode.ARABIC_ONLY, isPlaying = true, isBuffering = false)

        assertEquals(286, nowPlaying.ayahCount)
        assertEquals(0.5f, nowPlaying.progress, 0.001f)
        assertEquals(0f, nowPlaying.copy(ayah = 0).progress)
        assertEquals(PlaybackSpeed.X1, nowPlaying.speed)
        assertEquals(RepeatSetting.Off, nowPlaying.repeat)
    }
}
