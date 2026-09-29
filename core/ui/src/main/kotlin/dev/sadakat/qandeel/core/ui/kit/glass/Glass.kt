// The backdrop is provided once per window by the app shell, like a theme value.
@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package dev.sadakat.qandeel.core.ui.kit.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.sadakat.qandeel.core.designsystem.QandeelTheme

/*
 * The only file that talks to Haze, the backdrop-blur library: the rest of the app sees a backdrop,
 * a source modifier and a glass modifier, so the library can be swapped or upgraded here alone.
 */

/** What frosted chrome blurs: the pages under it. One per window, provided by the app shell. */
@Stable
class QandeelBackdrop internal constructor(internal val haze: HazeState)

/** The window's backdrop; null outside the app shell (sheets, dialogs, previews): no blur there. */
val LocalQandeelBackdrop = staticCompositionLocalOf<QandeelBackdrop?> { null }

@Composable
fun rememberQandeelBackdrop(): QandeelBackdrop = remember { QandeelBackdrop(HazeState()) }

/** Marks this content as part of the page that frosted chrome blurs. */
fun Modifier.qitBackdropSource(backdrop: QandeelBackdrop?): Modifier =
    if (backdrop == null) this else hazeSource(backdrop.haze)

/** What a surface is, which decides how see-through a glass look makes it. */
enum class QandeelSurfaceRole {
    /** The page itself and anything that must stay solid: never translucent. */
    PAGE,

    /** Bars, the mini player and chips floating over the page. */
    CHROME,

    /** Cards on the page. */
    CARD,

    /** Sheets, dialogs and menus: windows of their own. */
    SHEET,
}

/** How one translucent surface is drawn: its [tint] over the page, blurred by [blur] (0 = unblurred). */
@Immutable
data class QandeelGlassLook(
    val tint: Color,
    val blur: Dp,
    val noise: Float,
    val hairline: Dp,
    val hairlineColor: Color,
    /** Drawn under the blurred backdrop, where the page has nothing to show. */
    val page: Color,
)

/**
 * The glass look of a [role] surface whose solid color is [color], in the current theme and
 * [QandeelSurfaceMode]; null when the surface is drawn solid.
 */
@Composable
@ReadOnlyComposable
fun glassLook(color: Color, role: QandeelSurfaceRole): QandeelGlassLook? {
    val mode = LocalQandeelSurfaceMode.current
    val surfaces = QandeelTheme.surfaces
    if (role == QandeelSurfaceRole.PAGE || mode == QandeelSurfaceMode.OPAQUE || !surfaces.isTranslucent) return null
    val frosted = mode == QandeelSurfaceMode.FROSTED
    val alpha = when (role) {
        QandeelSurfaceRole.CHROME -> if (frosted) surfaces.chromeAlpha else surfaces.chromeFallbackAlpha
        QandeelSurfaceRole.CARD -> surfaces.cardAlpha
        else -> surfaces.sheetAlpha
    }
    val colors = QandeelTheme.colors
    // A light edge catches the light on glass: white on light pages, the ink on dark ones.
    val edge = if (colors.isDark) colors.onSurface else colors.surfaceContainerLowest
    return QandeelGlassLook(
        tint = color.copy(alpha = color.alpha * alpha),
        // Only chrome blurs: cards sit inside the page they would blur, and sheets are other windows.
        blur = if (frosted && role == QandeelSurfaceRole.CHROME) surfaces.blurRadius else 0.dp,
        noise = surfaces.noise,
        hairline = surfaces.hairline,
        hairlineColor = edge.copy(alpha = surfaces.hairlineAlpha),
        page = colors.background,
    )
}

/** Where a glass surface shows its light edge: all round, or only the side facing the page. */
enum class QandeelGlassEdge { ALL, TOP, BOTTOM }

/**
 * Draws [look] as this element's surface, clipped to [shape]: the blurred [backdrop] under the tint
 * when the look blurs and a backdrop exists, the tint over the page otherwise, and the light [edge].
 */
@OptIn(ExperimentalHazeApi::class)
fun Modifier.qitGlass(
    look: QandeelGlassLook,
    shape: Shape,
    backdrop: QandeelBackdrop?,
    edge: QandeelGlassEdge = QandeelGlassEdge.ALL,
): Modifier {
    val body = if (look.blur > 0.dp && backdrop != null) {
        clip(shape).hazeEffect(
            state = backdrop.haze,
            style = HazeStyle(
                backgroundColor = look.page,
                tint = HazeTint(look.tint),
                blurRadius = look.blur,
                noiseFactor = look.noise,
            ),
        ) {
            blurEnabled = true
            // Blur a downscaled copy: the same look for a fraction of the GPU work.
            inputScale = HazeInputScale.Auto
        }
    } else {
        background(look.tint, shape)
    }
    if (look.hairline <= 0.dp) return body
    return when (edge) {
        QandeelGlassEdge.ALL -> body.border(look.hairline, look.hairlineColor, shape)

        QandeelGlassEdge.TOP, QandeelGlassEdge.BOTTOM -> body.drawWithContent {
            drawContent()
            val stroke = look.hairline.toPx()
            val y = if (edge == QandeelGlassEdge.TOP) stroke / 2 else size.height - stroke / 2
            drawLine(look.hairlineColor, Offset(0f, y), Offset(size.width, y), stroke)
        }
    }
}
