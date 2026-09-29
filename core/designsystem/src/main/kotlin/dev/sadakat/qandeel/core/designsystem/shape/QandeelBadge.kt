package dev.sadakat.qandeel.core.designsystem.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape

/** The outline around surah and ayah numbers. */
enum class QandeelBadgeShape { OCTAGRAM, CIRCLE, COOKIE }

/**
 * The number badge: its [shape], drawn as an ornament line ([filled] false) or as a solid tonal
 * container ([filled] true).
 */
@Immutable
data class QandeelBadge(val shape: QandeelBadgeShape = QandeelBadgeShape.OCTAGRAM, val filled: Boolean = false)

/** The [Shape] that draws this outline. */
val QandeelBadgeShape.shape: Shape
    get() = when (this) {
        QandeelBadgeShape.OCTAGRAM -> OctagramShape
        QandeelBadgeShape.CIRCLE -> CircleBadgeShape
        QandeelBadgeShape.COOKIE -> CookieShape
    }
