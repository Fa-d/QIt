package dev.sadakat.qit.core.designsystem.scale

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fixed component sizes shared across screens. */
@Immutable
data class QItSizes(
    /** Minimum touch target (accessibility). */
    val touchTarget: Dp = 48.dp,
    val iconSmall: Dp = 18.dp,
    val icon: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    /** Octagram around a surah number in lists. */
    val numberBadge: Dp = 40.dp,
    /** Octagram around an ayah number in the reader. */
    val numberBadgeSmall: Dp = 34.dp,
    /** Stroke of small progress indicators. */
    val strokeThin: Dp = 2.dp,
    /** Line work of ornaments (the octagram). */
    val ornamentStroke: Dp = 1.5.dp,
)
