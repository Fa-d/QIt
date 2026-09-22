package dev.sadakat.qit.core.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WordTimingsTest {

    // 1:1 in the bundled timings: four words.
    private val basmala = WordTimings(intArrayOf(60, 610, 620, 1310, 1320, 2450, 2460, 5970))

    @Test
    fun `no word before the first starts`() {
        assertEquals(-1, basmala.wordAt(0))
        assertEquals(-1, basmala.wordAt(59))
    }

    @Test
    fun `the word whose span holds the position`() {
        assertEquals(0, basmala.wordAt(60))
        assertEquals(1, basmala.wordAt(1000))
        assertEquals(3, basmala.wordAt(5000))
    }

    @Test
    fun `a pause between words keeps the word just recited`() {
        assertEquals(0, basmala.wordAt(615))
    }

    @Test
    fun `the last word stays after the recitation`() {
        assertEquals(3, basmala.wordAt(9000))
    }

    @Test
    fun `exposes each word's span`() {
        assertEquals(4, basmala.wordCount)
        assertEquals(1320, basmala.startMs(2))
        assertEquals(2450, basmala.endMs(2))
    }

    @Test
    fun `an ayah without timings has no word`() {
        assertEquals(-1, WordTimings(IntArray(0)).wordAt(100))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `timings come in pairs`() {
        WordTimings(intArrayOf(1, 2, 3))
    }

    @Test
    fun `equal by content`() {
        assertEquals(WordTimings(intArrayOf(1, 2)), WordTimings(intArrayOf(1, 2)))
        assertEquals(WordTimings(intArrayOf(1, 2)).hashCode(), WordTimings(intArrayOf(1, 2)).hashCode())
        assertNotEquals(WordTimings(intArrayOf(1, 2)), WordTimings(intArrayOf(1, 3)))
        assertNotEquals(WordTimings(intArrayOf(1, 2)), "1, 2")
        assertEquals("WordTimings([1, 2])", WordTimings(intArrayOf(1, 2)).toString())
    }
}
