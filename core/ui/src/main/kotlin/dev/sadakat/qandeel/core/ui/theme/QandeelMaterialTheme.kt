package dev.sadakat.qandeel.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import dev.sadakat.qandeel.core.designsystem.ProvideQandeelTokens
import dev.sadakat.qandeel.core.designsystem.color.QandeelColors
import dev.sadakat.qandeel.core.designsystem.scale.QandeelRadius
import dev.sadakat.qandeel.core.designsystem.skin.QandeelSkin
import dev.sadakat.qandeel.core.designsystem.type.QandeelUiType
import dev.sadakat.qandeel.core.ui.kit.glass.LocalQandeelSurfaceMode
import dev.sadakat.qandeel.core.ui.kit.glass.QandeelSurfaceMode
import dev.sadakat.qandeel.core.ui.kit.glass.rememberSurfaceMode

/**
 * [skin]'s tokens, mapped onto Material 3 so Material components and Qandeel's own composables draw
 * from the same colors, type and shapes.
 *
 * @param arabicScale the reader's Arabic text size, applied to the reading styles.
 * @param surfaceMode how glass is drawn; null resolves it from the device (blur support, battery
 *   saver, contrast settings). Tests and previews pin it.
 */
@Composable
fun QandeelMaterialTheme(
    skin: QandeelSkin,
    arabicScale: Float = 1f,
    surfaceMode: QandeelSurfaceMode? = null,
    content: @Composable () -> Unit,
) {
    val scheme = remember(skin.colors) { skin.colors.toColorScheme() }
    val typography = remember(skin.type) { skin.type.toTypography() }
    val shapes = remember(skin.radius) { skin.radius.toShapes() }
    val mode = surfaceMode ?: rememberSurfaceMode(skin.surfaces)
    ProvideQandeelTokens(skin = skin, arabicScale = arabicScale) {
        MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes) {
            CompositionLocalProvider(LocalQandeelSurfaceMode provides mode, content = content)
        }
    }
}

/** The Material roles, 1:1 from Qandeel's. */
fun QandeelColors.toColorScheme(): ColorScheme {
    val scheme = if (isDark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        surfaceDim = surfaceDim,
        surfaceBright = surfaceBright,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
    )
}

/** Material's type scale, 1:1 from the look's. */
fun QandeelUiType.toTypography(): Typography = Typography(
    displayLarge = displayLarge,
    displayMedium = displayMedium,
    displaySmall = displaySmall,
    headlineLarge = headlineLarge,
    headlineMedium = headlineMedium,
    headlineSmall = headlineSmall,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = titleSmall,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = labelSmall,
)

/** Material's shape scale from the look's corner radii. */
fun QandeelRadius.toShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(xs),
    small = RoundedCornerShape(sm),
    medium = RoundedCornerShape(md),
    large = RoundedCornerShape(lg),
    extraLarge = RoundedCornerShape(xl),
)

/**
 * The look's roles with the wallpaper [scheme]'s accents: the primary, secondary and tertiary
 * families, and the extended roles derived from them, so a gold highlight never clashes with, say,
 * a blue wallpaper palette.
 *
 * @param keepPage keep the look's own page: its surfaces, ink and outlines. Sepia does, so the
 *   warm paper stays; otherwise the wallpaper's neutrals replace them too.
 */
fun QandeelColors.withWallpaper(scheme: ColorScheme, keepPage: Boolean = false): QandeelColors {
    val accents = copy(
        primary = scheme.primary,
        onPrimary = scheme.onPrimary,
        primaryContainer = scheme.primaryContainer,
        onPrimaryContainer = scheme.onPrimaryContainer,
        inversePrimary = scheme.inversePrimary,
        secondary = scheme.secondary,
        onSecondary = scheme.onSecondary,
        secondaryContainer = scheme.secondaryContainer,
        onSecondaryContainer = scheme.onSecondaryContainer,
        tertiary = scheme.tertiary,
        onTertiary = scheme.onTertiary,
        tertiaryContainer = scheme.tertiaryContainer,
        onTertiaryContainer = scheme.onTertiaryContainer,
        error = scheme.error,
        onError = scheme.onError,
        errorContainer = scheme.errorContainer,
        onErrorContainer = scheme.onErrorContainer,
        playingAyahHighlight = scheme.tertiaryContainer,
        onPlayingAyahHighlight = scheme.onTertiaryContainer,
        currentWordHighlight = scheme.primary,
        onCurrentWordHighlight = scheme.onPrimary,
        // Quieter than the recited words, which are onTertiaryContainer in full.
        upcomingWordOnHighlight = scheme.onTertiaryContainer.copy(alpha = UPCOMING_ON_HIGHLIGHT_ALPHA),
        currentWord = scheme.primary,
        currentWordOnHighlight = scheme.primary,
        ornament = scheme.tertiary,
        progressTrack = scheme.secondaryContainer,
    )
    if (keepPage) return accents
    return accents.copy(
        background = scheme.background,
        onBackground = scheme.onBackground,
        surface = scheme.surface,
        onSurface = scheme.onSurface,
        surfaceVariant = scheme.surfaceVariant,
        onSurfaceVariant = scheme.onSurfaceVariant,
        surfaceDim = scheme.surfaceDim,
        surfaceBright = scheme.surfaceBright,
        surfaceContainerLowest = scheme.surfaceContainerLowest,
        surfaceContainerLow = scheme.surfaceContainerLow,
        surfaceContainer = scheme.surfaceContainer,
        surfaceContainerHigh = scheme.surfaceContainerHigh,
        surfaceContainerHighest = scheme.surfaceContainerHighest,
        inverseSurface = scheme.inverseSurface,
        inverseOnSurface = scheme.inverseOnSurface,
        outline = scheme.outline,
        outlineVariant = scheme.outlineVariant,
        scrim = scheme.scrim,
        arabicText = scheme.onSurface,
        translationText = scheme.onSurfaceVariant,
        upcomingWord = scheme.onSurfaceVariant,
        divider = scheme.outlineVariant,
    )
}

/** Upcoming words on the wallpaper's highlight: its full ink, faded, as the brand schemes do with a lighter tone. */
private const val UPCOMING_ON_HIGHLIGHT_ALPHA = 0.7f
