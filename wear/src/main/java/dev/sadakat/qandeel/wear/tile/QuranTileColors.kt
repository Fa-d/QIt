package dev.sadakat.qandeel.wear.tile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.argb
import dev.sadakat.qandeel.wear.presentation.theme.QandeelWearColorScheme

/**
 * The app's watch colors for the tile. Tiles render outside Compose (in the system's tile host), so
 * they take ARGB values; this mirrors every role of the watch's Wear Material 3 scheme.
 */
internal val QuranTileColorScheme: ColorScheme = with(QandeelWearColorScheme) {
    ColorScheme(
        primary = primary.layout,
        primaryDim = primaryDim.layout,
        primaryContainer = primaryContainer.layout,
        onPrimary = onPrimary.layout,
        onPrimaryContainer = onPrimaryContainer.layout,
        secondary = secondary.layout,
        secondaryDim = secondaryDim.layout,
        secondaryContainer = secondaryContainer.layout,
        onSecondary = onSecondary.layout,
        onSecondaryContainer = onSecondaryContainer.layout,
        tertiary = tertiary.layout,
        tertiaryDim = tertiaryDim.layout,
        tertiaryContainer = tertiaryContainer.layout,
        onTertiary = onTertiary.layout,
        onTertiaryContainer = onTertiaryContainer.layout,
        surfaceContainerLow = surfaceContainerLow.layout,
        surfaceContainer = surfaceContainer.layout,
        surfaceContainerHigh = surfaceContainerHigh.layout,
        onSurface = onSurface.layout,
        onSurfaceVariant = onSurfaceVariant.layout,
        outline = outline.layout,
        outlineVariant = outlineVariant.layout,
        background = background.layout,
        onBackground = onBackground.layout,
        error = error.layout,
        errorDim = errorDim.layout,
        errorContainer = errorContainer.layout,
        onError = onError.layout,
        onErrorContainer = onErrorContainer.layout,
    )
}

private val Color.layout: LayoutColor get() = toArgb().argb
