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
 * The rub el hizb: an eight-pointed star made of two overlapping squares, the mark printed mushafs
 * use for sections and verse numbers. QIt draws it around surah and ayah numbers.
 */
object OctagramShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val points = octagramPoints(size)
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        return Outline.Generic(path)
    }
}

private const val POINTS = 8

/**
 * The 16 vertices of the star inscribed in [size], clockwise from the top point: the 8 tips on the
 * outer circle alternate with the 8 notches where the two squares' edges cross. For two squares the
 * notch radius is `cos(45°) / cos(22.5°)` of the tip radius.
 */
internal fun octagramPoints(size: Size): List<Offset> {
    val center = Offset(size.width / 2f, size.height / 2f)
    val outer = min(size.width, size.height) / 2f
    val inner = outer * (cos(PI / 4) / cos(PI / 8)).toFloat()
    val step = PI / POINTS
    return (0 until POINTS * 2).map { i ->
        val radius = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * step
        Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
    }
}
