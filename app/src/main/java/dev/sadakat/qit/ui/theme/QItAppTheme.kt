package dev.sadakat.qit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.sadakat.qit.core.designsystem.ProvideQItTokens
import dev.sadakat.qit.core.designsystem.color.QItColors
import dev.sadakat.qit.core.designsystem.color.darkQItColors
import dev.sadakat.qit.core.designsystem.color.lightQItColors
import dev.sadakat.qit.core.designsystem.scale.QItRadius
import dev.sadakat.qit.core.designsystem.type.QItUiType

/**
 * The phone's theme: QIt's tokens mapped onto Material 3, so Material components and our own
 * composables draw from the same colors, type and shapes.
 *
 * @param dynamicColor use the wallpaper colors (Android 12+) instead of the brand's; the extended
 *   roles (ayah highlight, ornaments, ...) are then derived from the wallpaper scheme too.
 * @param arabicScale the reader's Arabic text size, applied to the reading styles.
 */
@Composable
fun QItAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    arabicScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = remember(darkTheme, dynamicColor) {
        val brand = if (darkTheme) darkQItColors() else lightQItColors()
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wallpaper = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            brand.withDynamicColors(wallpaper)
        } else {
            brand
        }
    }
    ProvideQItTokens(colors = colors, arabicScale = arabicScale) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = QItTypography,
            shapes = QItShapes,
            content = content,
        )
    }
}

private val QItTypography = QItUiType().let { type ->
    Typography(
        displayLarge = type.displayLarge,
        displayMedium = type.displayMedium,
        displaySmall = type.displaySmall,
        headlineLarge = type.headlineLarge,
        headlineMedium = type.headlineMedium,
        headlineSmall = type.headlineSmall,
        titleLarge = type.titleLarge,
        titleMedium = type.titleMedium,
        titleSmall = type.titleSmall,
        bodyLarge = type.bodyLarge,
        bodyMedium = type.bodyMedium,
        bodySmall = type.bodySmall,
        labelLarge = type.labelLarge,
        labelMedium = type.labelMedium,
        labelSmall = type.labelSmall,
    )
}

private val QItShapes = QItRadius().let { radius ->
    Shapes(
        extraSmall = RoundedCornerShape(radius.xs),
        small = RoundedCornerShape(radius.sm),
        medium = RoundedCornerShape(radius.md),
        large = RoundedCornerShape(radius.lg),
        extraLarge = RoundedCornerShape(radius.xl),
    )
}

/** The Material roles, 1:1 from QIt's. */
internal fun QItColors.toColorScheme(): ColorScheme {
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

/**
 * The brand's roles replaced by the wallpaper [scheme]'s; the extended roles are derived from it so a
 * gold highlight never clashes with, say, a blue wallpaper palette.
 */
internal fun QItColors.withDynamicColors(scheme: ColorScheme): QItColors = copy(
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
    error = scheme.error,
    onError = scheme.onError,
    errorContainer = scheme.errorContainer,
    onErrorContainer = scheme.onErrorContainer,
    arabicText = scheme.onSurface,
    translationText = scheme.onSurfaceVariant,
    playingAyahHighlight = scheme.tertiaryContainer,
    onPlayingAyahHighlight = scheme.onTertiaryContainer,
    currentWord = scheme.primary,
    upcomingWord = scheme.onSurfaceVariant,
    currentWordOnHighlight = scheme.primary,
    upcomingWordOnHighlight = scheme.onTertiaryContainer,
    ornament = scheme.tertiary,
    progressTrack = scheme.secondaryContainer,
    divider = scheme.outlineVariant,
)
