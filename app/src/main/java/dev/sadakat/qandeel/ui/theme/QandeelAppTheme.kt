package dev.sadakat.qandeel.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.sadakat.qandeel.core.designsystem.skin.QandeelSkins
import dev.sadakat.qandeel.core.designsystem.skin.QandeelStyle
import dev.sadakat.qandeel.core.designsystem.skin.QandeelTone
import dev.sadakat.qandeel.core.domain.model.UiStyle
import dev.sadakat.qandeel.core.ui.kit.glass.QandeelSurfaceMode
import dev.sadakat.qandeel.core.ui.theme.QandeelMaterialTheme
import dev.sadakat.qandeel.core.ui.theme.withWallpaper

/**
 * The phone's theme: the skin of [style] on [tone], mapped onto Material 3 by the UI kit.
 *
 * @param dynamicColor use the wallpaper's accent colors (Android 12+) instead of the look's; the
 *   extended roles (ayah highlight, ornaments, ...) follow them. A sepia page keeps its paper.
 * @param arabicScale the reader's Arabic text size, applied to the reading styles.
 * @param surfaceMode pins how glass is drawn (tests); null follows the device.
 */
@Composable
fun QandeelAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    arabicScale: Float = 1f,
    style: QandeelStyle = QandeelStyle.MUSHAF,
    tone: QandeelTone = if (darkTheme) QandeelTone.DARK else QandeelTone.LIGHT,
    surfaceMode: QandeelSurfaceMode? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val skin = remember(style, tone, dynamicColor, configuration) {
        val skin = QandeelSkins.of(style, tone)
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wallpaper = if (tone ==
                QandeelTone.DARK
            ) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            skin.copy(colors = skin.colors.withWallpaper(wallpaper, keepPage = tone == QandeelTone.SEPIA))
        } else {
            skin
        }
    }
    QandeelMaterialTheme(skin = skin, arabicScale = arabicScale, surfaceMode = surfaceMode, content = content)
}

/** The design system's style for the stored [UiStyle]. */
fun UiStyle.toQandeelStyle(): QandeelStyle = when (this) {
    UiStyle.MUSHAF -> QandeelStyle.MUSHAF
    UiStyle.MATERIAL -> QandeelStyle.MATERIAL
    UiStyle.EXPRESSIVE -> QandeelStyle.EXPRESSIVE
    UiStyle.GLASS -> QandeelStyle.GLASS
}
