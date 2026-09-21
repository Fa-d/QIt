package dev.sadakat.qit.core.designsystem.scale

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tonal elevation levels: how far a surface lifts off the page (shown as tint, not shadow). */
@Immutable
data class QItElevation(val level0: Dp = 0.dp, val level1: Dp = 1.dp, val level2: Dp = 3.dp, val level3: Dp = 6.dp)
