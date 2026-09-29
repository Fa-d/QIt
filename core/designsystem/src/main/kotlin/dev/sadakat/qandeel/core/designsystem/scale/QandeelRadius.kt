package dev.sadakat.qandeel.core.designsystem.scale

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii, matching Material's shape scale from extra small to extra large. */
@Immutable
data class QandeelRadius(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 28.dp,
)
