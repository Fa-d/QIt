package dev.sadakat.qandeel.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import dev.sadakat.qandeel.core.designsystem.ProvideQandeelTokens
import dev.sadakat.qandeel.core.designsystem.color.watchQandeelColors
import dev.sadakat.qandeel.core.designsystem.ref.QandeelPalettes

/**
 * The watch's theme: Qandeel's palettes on Wear Material 3's roles (with the tones Wear's own baseline
 * scheme uses), on a black background. The watch always wears the brand colors: it is the one place
 * the mushaf palette is guaranteed to be seen at a glance.
 */
@Composable
fun QandeelWearTheme(content: @Composable () -> Unit) {
    ProvideQandeelTokens(colors = watchQandeelColors()) {
        MaterialTheme(colorScheme = QandeelWearColorScheme, content = content)
    }
}

internal val QandeelWearColorScheme: ColorScheme = with(QandeelPalettes) {
    ColorScheme(
        primary = primary[90],
        primaryDim = primary[80],
        primaryContainer = primary[30],
        onPrimary = primary[10],
        onPrimaryContainer = primary[95],
        secondary = secondary[90],
        secondaryDim = secondary[80],
        secondaryContainer = secondary[30],
        onSecondary = secondary[10],
        onSecondaryContainer = secondary[95],
        tertiary = tertiary[90],
        tertiaryDim = tertiary[80],
        tertiaryContainer = tertiary[30],
        onTertiary = tertiary[10],
        onTertiaryContainer = tertiary[95],
        surfaceContainerLow = neutral[15],
        surfaceContainer = neutral[20],
        surfaceContainerHigh = neutral[30],
        onSurface = neutral[95],
        onSurfaceVariant = neutralVariant[80],
        outline = neutralVariant[60],
        outlineVariant = neutralVariant[40],
        background = neutral[0],
        onBackground = neutral[100],
        error = error[80],
        errorDim = error[70],
        errorContainer = error[30],
        onError = error[10],
        onErrorContainer = error[95],
    )
}
