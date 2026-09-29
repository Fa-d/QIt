package dev.sadakat.qit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.sadakat.qit.core.designsystem.skin.QItSkins
import dev.sadakat.qit.core.designsystem.skin.QItStyle
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.core.domain.model.UiStyle
import dev.sadakat.qit.core.ui.kit.glass.QItSurfaceMode
import dev.sadakat.qit.core.ui.theme.QItMaterialTheme
import dev.sadakat.qit.core.ui.theme.withWallpaper

/**
 * The phone's theme: the skin of [style] on [tone], mapped onto Material 3 by the UI kit.
 *
 * @param dynamicColor use the wallpaper's accent colors (Android 12+) instead of the look's; the
 *   extended roles (ayah highlight, ornaments, ...) follow them. A sepia page keeps its paper.
 * @param arabicScale the reader's Arabic text size, applied to the reading styles.
 * @param surfaceMode pins how glass is drawn (tests); null follows the device.
 */
@Composable
fun QItAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    arabicScale: Float = 1f,
    style: QItStyle = QItStyle.MUSHAF,
    tone: QItTone = if (darkTheme) QItTone.DARK else QItTone.LIGHT,
    surfaceMode: QItSurfaceMode? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val skin = remember(style, tone, dynamicColor, configuration) {
        val skin = QItSkins.of(style, tone)
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wallpaper = if (tone ==
                QItTone.DARK
            ) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            skin.copy(colors = skin.colors.withWallpaper(wallpaper, keepPage = tone == QItTone.SEPIA))
        } else {
            skin
        }
    }
    QItMaterialTheme(skin = skin, arabicScale = arabicScale, surfaceMode = surfaceMode, content = content)
}

/** The design system's style for the stored [UiStyle]. */
fun UiStyle.toQItStyle(): QItStyle = when (this) {
    UiStyle.MUSHAF -> QItStyle.MUSHAF
    UiStyle.MATERIAL -> QItStyle.MATERIAL
    UiStyle.EXPRESSIVE -> QItStyle.EXPRESSIVE
    UiStyle.GLASS -> QItStyle.GLASS
}
