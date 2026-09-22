package dev.sadakat.qit.core.designsystem.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape

/** The outline around surah and ayah numbers. */
enum class QItBadgeShape { OCTAGRAM, CIRCLE, COOKIE }

/**
 * The number badge: its [shape], drawn as an ornament line ([filled] false) or as a solid tonal
 * container ([filled] true).
 */
@Immutable
data class QItBadge(val shape: QItBadgeShape = QItBadgeShape.OCTAGRAM, val filled: Boolean = false)

/** The [Shape] that draws this outline. */
val QItBadgeShape.shape: Shape
    get() = when (this) {
        QItBadgeShape.OCTAGRAM -> OctagramShape
        QItBadgeShape.CIRCLE -> CircleBadgeShape
        QItBadgeShape.COOKIE -> CookieShape
    }
