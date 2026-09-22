package dev.sadakat.qit.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.sadakat.qit.core.designsystem.color.lightQItColors
import dev.sadakat.qit.core.designsystem.scale.QItRadius
import dev.sadakat.qit.core.designsystem.skin.QItSkins
import dev.sadakat.qit.core.designsystem.skin.QItStyle
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.core.designsystem.type.QItUiType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class QItMaterialThemeTest {

    private val brand = lightQItColors()
    private val wallpaper = darkColorScheme(
        primary = Color.Blue,
        tertiaryContainer = Color.Cyan,
        surface = Color.DarkGray,
        onSurface = Color.White,
    )

    @Test
    fun `Material's roles are QIt's, one to one`() {
        val scheme = brand.toColorScheme()

        assertEquals(brand.primary, scheme.primary)
        assertEquals(brand.surfaceContainerHigh, scheme.surfaceContainerHigh)
        assertEquals(brand.onSurfaceVariant, scheme.onSurfaceVariant)
        assertEquals(brand.primary, scheme.surfaceTint)
    }

    @Test
    fun `type and shapes map from the look`() {
        val type = QItUiType.sans()
        assertEquals(type.headlineLarge, type.toTypography().headlineLarge)
        assertEquals(QItRadius(xl = QItRadius().xl).toShapes().extraLarge, QItRadius().toShapes().extraLarge)
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
        val light = QItSkins.of(QItStyle.MATERIAL, QItTone.LIGHT).colors.toColorScheme()
        val dark = QItSkins.of(QItStyle.MATERIAL, QItTone.DARK).colors.toColorScheme()

        assertEquals(lightColorScheme().primary, light.primary)
        assertEquals(lightColorScheme().onPrimary, light.onPrimary)
        assertEquals(darkColorScheme().onPrimary.toArgb() and RGB, dark.onPrimary.toArgb() and RGB)
    }

    private companion object {
        const val RGB = 0xFFFFFF
    }
}
