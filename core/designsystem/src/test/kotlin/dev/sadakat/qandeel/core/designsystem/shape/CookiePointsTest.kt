package dev.sadakat.qandeel.core.designsystem.shape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.hypot

class CookiePointsTest {

    private val center = Offset(50f, 50f)
    private val points = cookiePoints(Size(100f, 100f))
    private val radii = points.map { hypot(it.x - center.x, it.y - center.y) }

    @Test
    fun `it starts at the top of a lobe`() {
        assertEquals(50f, points.first().x, TOLERANCE)
        assertEquals(0f, points.first().y, TOLERANCE)
    }

    @Test
    fun `lobes touch the bounds and notches cut in by the cookie's depth`() {
        assertEquals(50f, radii.max(), TOLERANCE)
        assertEquals(50f * (1 - COOKIE_DEPTH), radii.min(), TOLERANCE)
    }

    @Test
    fun `it has nine lobes`() {
        val tips = radii.indices.count { i ->
            radii[i] > radii[(i + 1) % radii.size] &&
                radii[i] >= radii[(i - 1 + radii.size) % radii.size]
        }
        assertEquals(COOKIE_LOBES, tips)
    }

    @Test
    fun `every badge shape has an outline`() {
        assertEquals(OctagramShape, QandeelBadgeShape.OCTAGRAM.shape)
        assertEquals(CircleBadgeShape, QandeelBadgeShape.CIRCLE.shape)
        assertEquals(CookieShape, QandeelBadgeShape.COOKIE.shape)
    }

    private companion object {
        const val TOLERANCE = 0.01f
    }
}
