package dev.sadakat.qandeel.core.ui.kit

import org.junit.Assert.assertEquals
import org.junit.Test

/** A 1000 px list with a 100 px bar over its top and a 200 px mini player over its bottom. */
class PaddedBringIntoViewSpecTest {

    private val spec = PaddedBringIntoViewSpec(top = 100f, bottom = 200f)

    private fun distance(offset: Float, size: Float) = spec.calculateScrollDistance(offset, size, CONTAINER)

    @Test
    fun `an item clear of both bars stays put`() {
        assertEquals(0f, distance(offset = 100f, size = 700f), DELTA)
        assertEquals(0f, distance(offset = 400f, size = 50f), DELTA)
    }

    @Test
    fun `an item under the top bar scrolls down just below it`() {
        assertEquals(-60f, distance(offset = 40f, size = 50f), DELTA)
    }

    @Test
    fun `an item under the mini player scrolls up just above it`() {
        // Its bottom at 850 must come up to 800.
        assertEquals(50f, distance(offset = 780f, size = 70f), DELTA)
    }

    @Test
    fun `an item taller than the clear space and covering it stays put`() {
        assertEquals(0f, distance(offset = 50f, size = 900f), DELTA)
    }

    private companion object {
        const val CONTAINER = 1000f
        const val DELTA = 0.001f
    }
}
