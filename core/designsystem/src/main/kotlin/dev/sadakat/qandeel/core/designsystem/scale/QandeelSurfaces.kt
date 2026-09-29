package dev.sadakat.qandeel.core.designsystem.scale

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How surfaces are made: opaque paper, or frosted glass that lets the page show through. The
 * defaults are opaque, the look of every style but Glass. Alphas apply to the surface's own color.
 */
@Immutable
data class QandeelSurfaces(
    /** Bars, the mini player and chips that float over the page, when the backdrop is blurred. */
    val chromeAlpha: Float = 1f,
    /** The same chrome when blur is unavailable (before Android 12): more opaque, to stay readable. */
    val chromeFallbackAlpha: Float = 1f,
    /** Sheets and dialogs. They're windows of their own, so only the system can blur behind them. */
    val sheetAlpha: Float = 1f,
    /** Cards on the page. */
    val cardAlpha: Float = 1f,
    /** Blur of the backdrop behind translucent chrome; 0 means no blur. */
    val blurRadius: Dp = 0.dp,
    /** Grain on frosted surfaces, 0..1, so large blurs don't band. */
    val noise: Float = 0f,
    /** The light edge of glass; 0 means no edge. */
    val hairline: Dp = 0.dp,
    val hairlineAlpha: Float = 0f,
    /** Above 0, the mini player floats as a pill this far from the screen's edges. */
    val floatingInset: Dp = 0.dp,
    /** Shadow under floating chrome. */
    val shadow: Dp = 0.dp,
    /** Strength of the color wash behind pages of lists (never behind the Quran's text), 0..1. */
    val backdropWash: Float = 0f,
) {
    /** True when chrome lets the page show through. */
    val isTranslucent: Boolean get() = chromeAlpha < 1f
}
