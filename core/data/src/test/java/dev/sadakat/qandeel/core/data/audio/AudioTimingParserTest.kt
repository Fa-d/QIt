package dev.sadakat.qandeel.core.data.audio

import dev.sadakat.qandeel.core.domain.audio.WordTimings
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioTimingParserTest {

    @Test
    fun `word timings by ayah`() {
        val parsed = AudioTimingParser.parseWordTimings("[[1,[0,10,10,30]],[2,[5,9]]]")
        assertEquals(mapOf(1 to WordTimings(intArrayOf(0, 10, 10, 30)), 2 to WordTimings(intArrayOf(5, 9))), parsed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed json is rejected`() {
        AudioTimingParser.parseWordTimings("[[1,")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a row without timings is rejected`() {
        AudioTimingParser.parseWordTimings("[[1]]")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unpaired timings are rejected`() {
        AudioTimingParser.parseWordTimings("[[1,[0,10,20]]]")
    }

    @Test
    fun `durations by file id`() {
        val durations = AudioTimingParser.parseDurations(
            """{"verses":{"ar":[100,0],"bn.toha":[500]},"intros":{"bn":[0,400]}}""",
        )
        assertEquals(100L, durations.durationMs("ar/1"))
        assertEquals(null, durations.durationMs("ar/2")) // 0 = unknown
        assertEquals(500L, durations.durationMs("bn.toha/1"))
        assertEquals(400L, durations.durationMs("bn/intro/2"))
        assertEquals(null, durations.durationMs("bn/outro/2"))
        assertEquals(null, durations.durationMs("xx/1")) // a code with no durations
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed durations are rejected`() {
        AudioTimingParser.parseDurations("""{"verses":{}}""")
    }
}
