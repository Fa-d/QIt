package dev.sadakat.qandeel.core.designsystem.scale

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale on a 4 dp grid (plus a 2 dp step for hairline gaps). */
@Immutable
data class QandeelSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
) {
    /** Horizontal inset of screen content from the display edge. */
    val screenGutter: Dp get() = lg
}
