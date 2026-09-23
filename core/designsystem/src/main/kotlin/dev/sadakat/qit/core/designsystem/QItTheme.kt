// The token locals below are the design system itself, the one place CompositionLocals belong.
// (An editorconfig allowlist can't name them: ktlint lower-cases that property's value.)
@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package dev.sadakat.qit.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import dev.sadakat.qit.core.designsystem.color.QItColors
import dev.sadakat.qit.core.designsystem.color.lightQItColors
import dev.sadakat.qit.core.designsystem.scale.QItElevation
import dev.sadakat.qit.core.designsystem.scale.QItMotion
import dev.sadakat.qit.core.designsystem.scale.QItRadius
import dev.sadakat.qit.core.designsystem.scale.QItSizes
import dev.sadakat.qit.core.designsystem.scale.QItSpacing
import dev.sadakat.qit.core.designsystem.scale.QItSurfaces
import dev.sadakat.qit.core.designsystem.shape.QItBadge
import dev.sadakat.qit.core.designsystem.skin.QItSkin
import dev.sadakat.qit.core.designsystem.type.QItArabicType
import dev.sadakat.qit.core.designsystem.type.QItUiType

// Static locals: the values change only when the theme or the text size setting changes, and then
// everything below should recompose anyway.
private val LocalQItColors = staticCompositionLocalOf { lightQItColors() }
private val LocalQItSpacing = staticCompositionLocalOf { QItSpacing() }
private val LocalQItRadius = staticCompositionLocalOf { QItRadius() }
private val LocalQItElevation = staticCompositionLocalOf { QItElevation() }
private val LocalQItSizes = staticCompositionLocalOf { QItSizes() }
private val LocalQItMotion = staticCompositionLocalOf { QItMotion() }
private val LocalQItArabicType = staticCompositionLocalOf { QItArabicType() }
private val LocalQItUiType = staticCompositionLocalOf { QItUiType() }
private val LocalQItSurfaces = staticCompositionLocalOf { QItSurfaces() }
private val LocalQItBadge = staticCompositionLocalOf { QItBadge() }

/**
 * The design tokens of the current theme. Screens read these (and MaterialTheme's typography and
 * shapes, which the apps build from the same tokens) instead of spelling out colors and sizes.
 */
object QItTheme {
    val colors: QItColors
        @Composable @ReadOnlyComposable
        get() = LocalQItColors.current

    val spacing: QItSpacing
        @Composable @ReadOnlyComposable
        get() = LocalQItSpacing.current

    val radius: QItRadius
        @Composable @ReadOnlyComposable
        get() = LocalQItRadius.current

    val elevation: QItElevation
        @Composable @ReadOnlyComposable
        get() = LocalQItElevation.current

    val sizes: QItSizes
        @Composable @ReadOnlyComposable
        get() = LocalQItSizes.current

    val motion: QItMotion
        @Composable @ReadOnlyComposable
        get() = LocalQItMotion.current

    /** The Arabic styles, already scaled by the reader's text size setting. */
    val arabic: QItArabicType
        @Composable @ReadOnlyComposable
        get() = LocalQItArabicType.current

    /** The Latin type scale of the current look; the phone also maps it onto MaterialTheme. */
    val type: QItUiType
        @Composable @ReadOnlyComposable
        get() = LocalQItUiType.current

    /** How the current look makes its surfaces: opaque, or frosted glass. */
    val surfaces: QItSurfaces
        @Composable @ReadOnlyComposable
        get() = LocalQItSurfaces.current

    /** The outline around surah and ayah numbers in the current look. */
    val badge: QItBadge
        @Composable @ReadOnlyComposable
        get() = LocalQItBadge.current
}

/**
 * Provides every token of [skin] to [content], with the Arabic styles scaled by [arabicScale]. The
 * phone's theme calls this around its MaterialTheme.
 */
@Composable
fun ProvideQItTokens(skin: QItSkin, arabicScale: Float = 1f, content: @Composable () -> Unit) {
    val arabic = remember(arabicScale) { QItArabicType().scaled(arabicScale) }
    CompositionLocalProvider(
        LocalQItColors provides skin.colors,
        LocalQItUiType provides skin.type,
        LocalQItRadius provides skin.radius,
        LocalQItSurfaces provides skin.surfaces,
        LocalQItMotion provides skin.motion,
        LocalQItBadge provides skin.badge,
        LocalQItArabicType provides arabic,
        content = content,
    )
}

/**
 * Provides [colors] for the current light/dark/dynamic scheme and the Arabic styles scaled by
 * [arabicScale]; every other token keeps the brand's value. The watch's theme calls this: it always
 * wears the mushaf look.
 */
@Composable
fun ProvideQItTokens(colors: QItColors, arabicScale: Float = 1f, content: @Composable () -> Unit) {
    val arabic = remember(arabicScale) { QItArabicType().scaled(arabicScale) }
    CompositionLocalProvider(
        LocalQItColors provides colors,
        LocalQItArabicType provides arabic,
        content = content,
    )
}
