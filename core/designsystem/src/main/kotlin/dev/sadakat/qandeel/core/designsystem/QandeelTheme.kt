// The token locals below are the design system itself, the one place CompositionLocals belong.
// (An editorconfig allowlist can't name them: ktlint lower-cases that property's value.)
@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package dev.sadakat.qandeel.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import dev.sadakat.qandeel.core.designsystem.color.QandeelColors
import dev.sadakat.qandeel.core.designsystem.color.lightQandeelColors
import dev.sadakat.qandeel.core.designsystem.scale.QandeelElevation
import dev.sadakat.qandeel.core.designsystem.scale.QandeelMotion
import dev.sadakat.qandeel.core.designsystem.scale.QandeelRadius
import dev.sadakat.qandeel.core.designsystem.scale.QandeelSizes
import dev.sadakat.qandeel.core.designsystem.scale.QandeelSpacing
import dev.sadakat.qandeel.core.designsystem.scale.QandeelSurfaces
import dev.sadakat.qandeel.core.designsystem.shape.QandeelBadge
import dev.sadakat.qandeel.core.designsystem.skin.QandeelSkin
import dev.sadakat.qandeel.core.designsystem.type.QandeelArabicType
import dev.sadakat.qandeel.core.designsystem.type.QandeelUiType

// Static locals: the values change only when the theme or the text size setting changes, and then
// everything below should recompose anyway.
private val LocalQandeelColors = staticCompositionLocalOf { lightQandeelColors() }
private val LocalQandeelSpacing = staticCompositionLocalOf { QandeelSpacing() }
private val LocalQandeelRadius = staticCompositionLocalOf { QandeelRadius() }
private val LocalQandeelElevation = staticCompositionLocalOf { QandeelElevation() }
private val LocalQandeelSizes = staticCompositionLocalOf { QandeelSizes() }
private val LocalQandeelMotion = staticCompositionLocalOf { QandeelMotion() }
private val LocalQandeelArabicType = staticCompositionLocalOf { QandeelArabicType() }
private val LocalQandeelUiType = staticCompositionLocalOf { QandeelUiType() }
private val LocalQandeelSurfaces = staticCompositionLocalOf { QandeelSurfaces() }
private val LocalQandeelBadge = staticCompositionLocalOf { QandeelBadge() }

/**
 * The design tokens of the current theme. Screens read these (and MaterialTheme's typography and
 * shapes, which the apps build from the same tokens) instead of spelling out colors and sizes.
 */
object QandeelTheme {
    val colors: QandeelColors
        @Composable @ReadOnlyComposable
        get() = LocalQandeelColors.current

    val spacing: QandeelSpacing
        @Composable @ReadOnlyComposable
        get() = LocalQandeelSpacing.current

    val radius: QandeelRadius
        @Composable @ReadOnlyComposable
        get() = LocalQandeelRadius.current

    val elevation: QandeelElevation
        @Composable @ReadOnlyComposable
        get() = LocalQandeelElevation.current

    val sizes: QandeelSizes
        @Composable @ReadOnlyComposable
        get() = LocalQandeelSizes.current

    val motion: QandeelMotion
        @Composable @ReadOnlyComposable
        get() = LocalQandeelMotion.current

    /** The Arabic styles, already scaled by the reader's text size setting. */
    val arabic: QandeelArabicType
        @Composable @ReadOnlyComposable
        get() = LocalQandeelArabicType.current

    /** The Latin type scale of the current look; the phone also maps it onto MaterialTheme. */
    val type: QandeelUiType
        @Composable @ReadOnlyComposable
        get() = LocalQandeelUiType.current

    /** How the current look makes its surfaces: opaque, or frosted glass. */
    val surfaces: QandeelSurfaces
        @Composable @ReadOnlyComposable
        get() = LocalQandeelSurfaces.current

    /** The outline around surah and ayah numbers in the current look. */
    val badge: QandeelBadge
        @Composable @ReadOnlyComposable
        get() = LocalQandeelBadge.current
}

/**
 * Provides every token of [skin] to [content], with the Arabic styles scaled by [arabicScale]. The
 * phone's theme calls this around its MaterialTheme.
 */
@Composable
fun ProvideQandeelTokens(skin: QandeelSkin, arabicScale: Float = 1f, content: @Composable () -> Unit) {
    val arabic = remember(arabicScale) { QandeelArabicType().scaled(arabicScale) }
    CompositionLocalProvider(
        LocalQandeelColors provides skin.colors,
        LocalQandeelUiType provides skin.type,
        LocalQandeelRadius provides skin.radius,
        LocalQandeelSurfaces provides skin.surfaces,
        LocalQandeelMotion provides skin.motion,
        LocalQandeelBadge provides skin.badge,
        LocalQandeelArabicType provides arabic,
        content = content,
    )
}

/**
 * Provides [colors] for the current light/dark/dynamic scheme and the Arabic styles scaled by
 * [arabicScale]; every other token keeps the brand's value. The watch's theme calls this: it always
 * wears the mushaf look.
 */
@Composable
fun ProvideQandeelTokens(colors: QandeelColors, arabicScale: Float = 1f, content: @Composable () -> Unit) {
    val arabic = remember(arabicScale) { QandeelArabicType().scaled(arabicScale) }
    CompositionLocalProvider(
        LocalQandeelColors provides colors,
        LocalQandeelArabicType provides arabic,
        content = content,
    )
}
