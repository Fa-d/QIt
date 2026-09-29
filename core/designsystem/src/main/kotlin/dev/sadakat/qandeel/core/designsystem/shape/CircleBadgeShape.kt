package dev.sadakat.qandeel.core.designsystem.shape

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

/** A circle centered in the bounds (the design system has no foundation dependency for CircleShape). */
object CircleBadgeShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val diameter = min(size.width, size.height)
        val left = (size.width - diameter) / 2f
        val top = (size.height - diameter) / 2f
        val radius = CornerRadius(diameter / 2f)
        return Outline.Rounded(RoundRect(left, top, left + diameter, top + diameter, radius))
    }
}
