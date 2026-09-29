package dev.sadakat.qandeel.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.sadakat.qandeel.core.designsystem.color.lightQandeelColors
import dev.sadakat.qandeel.core.designsystem.scale.QandeelRadius
import dev.sadakat.qandeel.core.designsystem.skin.QandeelSkins
import dev.sadakat.qandeel.core.designsystem.skin.QandeelStyle
import dev.sadakat.qandeel.core.designsystem.skin.QandeelTone
import dev.sadakat.qandeel.core.designsystem.type.QandeelUiType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class QandeelMaterialThemeTest {

    private val brand = lightQandeelColors()
    private val wallpaper = darkColorScheme(
        primary = Color.Blue,
        tertiaryContainer = Color.Cyan,
        surface = Color.DarkGray,
        onSurface = Color.White,
    )

    @Test
    fun `Material's roles are Qandeel's, one to one`() {
        val scheme = brand.toColorScheme()

        assertEquals(brand.primary, scheme.primary)
        assertEquals(brand.surfaceContainerHigh, scheme.surfaceContainerHigh)
        assertEquals(brand.onSurfaceVariant, scheme.onSurfaceVariant)
        assertEquals(brand.primary, scheme.surfaceTint)
    }

    @Test
    fun `type and shapes map from the look`() {
        val type = QandeelUiType.sans()
        assertEquals(type.headlineLarge, type.toTypography().headlineLarge)
        assertEquals(
            QandeelRadius(xl = QandeelRadius().xl).toShapes().extraLarge,
            QandeelRadius().toShapes().extraLarge,
        )
    }

    @Test
    fun `the wallpaper replaces the accents and the page`() {
        val colors = brand.withWallpaper(wallpaper)

        assertEquals(Color.Blue, colors.primary)
        assertEquals(Color.Blue, colors.currentWordHighlight)
        assertEquals(Color.Cyan, colors.playingAyahHighlight)
        assertEquals(Color.DarkGray, colors.surface)
        assertEquals(Color.White, colors.arabicText)
    }

    @Test
    fun `a kept page takes only the wallpaper's accents`() {
        val colors = brand.withWallpaper(wallpaper, keepPage = true)

        assertEquals(Color.Blue, colors.primary)
        assertEquals(Color.Cyan, colors.playingAyahHighlight)
        assertEquals(brand.surface, colors.surface)
        assertEquals(brand.arabicText, colors.arabicText)
        assertNotEquals(lightColorScheme().surface, colors.surface)
    }

    @Test
    fun `the material style is Material 3's baseline`() {
        val light = QandeelSkins.of(QandeelStyle.MATERIAL, QandeelTone.LIGHT).colors.toColorScheme()
        val dark = QandeelSkins.of(QandeelStyle.MATERIAL, QandeelTone.DARK).colors.toColorScheme()

        assertEquals(lightColorScheme().primary, light.primary)
        assertEquals(lightColorScheme().onPrimary, light.onPrimary)
        assertEquals(darkColorScheme().onPrimary.toArgb() and RGB, dark.onPrimary.toArgb() and RGB)
    }

    private companion object {
        const val RGB = 0xFFFFFF
    }
}
