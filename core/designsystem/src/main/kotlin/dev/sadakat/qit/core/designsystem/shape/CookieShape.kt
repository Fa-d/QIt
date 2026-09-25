package dev.sadakat.qit.core.designsystem.shape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A scalloped circle with nine soft lobes, like Material 3 Expressive's "cookie": the Expressive
 * style's number badge. Drawn from a sampled outline so it needs no graphics-shapes dependency.
 */
object CookieShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val points = cookiePoints(size)
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        return Outline.Generic(path)
    }
}

internal const val COOKIE_LOBES = 9

/** How far the notches between lobes cut in, as a share of the radius. */
internal const val COOKIE_DEPTH = 0.12f

private const val SAMPLES_PER_LOBE = 16

/**
 * The outline inscribed in [size], clockwise from a lobe at the top: the radius swings on a cosine
 * between the full radius (lobe tips) and `1 - COOKIE_DEPTH` of it (notches).
 */
internal fun cookiePoints(size: Size): List<Offset> {
    val center = Offset(size.width / 2f, size.height / 2f)
    val outer = min(size.width, size.height) / 2f
    val samples = COOKIE_LOBES * SAMPLES_PER_LOBE
    return (0 until samples).map { i ->
        val t = 2 * PI * i / samples
        val swing = (1 - cos(COOKIE_LOBES * t)) / 2
        val radius = outer * (1 - COOKIE_DEPTH * swing.toFloat())
        val angle = -PI / 2 + t
        Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
    }
}
