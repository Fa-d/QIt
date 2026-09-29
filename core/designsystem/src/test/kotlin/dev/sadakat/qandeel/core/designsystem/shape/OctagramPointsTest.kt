package dev.sadakat.qandeel.core.designsystem.shape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.hypot

class OctagramPointsTest {

    private val size = Size(100f, 100f)
    private val center = Offset(50f, 50f)
    private val points = octagramPoints(size)

    @Test
    fun `the star has eight tips and eight notches`() {
        assertEquals(16, points.size)
    }

    @Test
    fun `it starts at the top tip`() {
        assertEquals(50f, points.first().x, TOLERANCE)
        assertEquals(0f, points.first().y, TOLERANCE)
    }

    @Test
    fun `tips touch the bounds and notches sit where the two squares cross`() {
        points.forEachIndexed { i, point ->
            val radius = hypot(point.x - center.x, point.y - center.y)
            val expected = if (i % 2 == 0) 50f else 50f * 0.76537f
            assertEquals("point $i", expected, radius, TOLERANCE)
        }
    }

    @Test
    fun `a non-square box fits the star in its shorter side, centered`() {
        val wide = octagramPoints(Size(200f, 100f))

        assertEquals(100f, wide.first().x, TOLERANCE)
        assertEquals(150f, wide.maxOf { it.x }, TOLERANCE)
        assertEquals(50f, wide.minOf { it.x }, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 0.01f
    }
}
