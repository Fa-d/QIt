package dev.sadakat.qit.core.domain.player

import dev.sadakat.qit.core.domain.audio.WordTimings
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class WordPointerTest {

    private val timings = mapOf(
        0 to WordTimings(intArrayOf(60, 610, 620, 1_310)),
        5 to WordTimings(intArrayOf(100, 900, 950, 2_000, 2_050, 3_000)),
    )

    private fun playing(ayah: Int, track: Track) =
        NowPlaying(18, ayah, track, RecitationMode.ARABIC_ENGLISH, isPlaying = true, isBuffering = false)

    private fun at(itemMs: Long) = PlaybackProgress(itemMs, surahPositionMs = 0, surahDurationMs = 0)

    @Test
    fun `follows the recited word`() {
        assertEquals(WordPointer.Reciting(-1), WordPointer.of(playing(5, Track.ARABIC), at(50), timings))
        assertEquals(WordPointer.Reciting(1), WordPointer.of(playing(5, Track.ARABIC), at(1_000), timings))
        assertEquals(WordPointer.Reciting(2), WordPointer.of(playing(5, Track.ARABIC), at(9_000), timings))
    }

    @Test
    fun `the basmala has its own words`() {
        assertEquals(WordPointer.Reciting(1), WordPointer.of(playing(0, Track.ARABIC), at(700), timings))
    }

    @Test
    fun `a translation steps the arabic back`() {
        assertEquals(WordPointer.Translating, WordPointer.of(playing(5, Track.ENGLISH), at(700), timings))
    }

    @Test
    fun `nothing to point at without playback or timings`() {
        assertEquals(WordPointer.Off, WordPointer.of(null, at(0), timings))
        assertEquals(WordPointer.Off, WordPointer.of(playing(6, Track.ARABIC), at(700), timings))
    }
}
