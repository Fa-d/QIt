package dev.sadakat.qandeel.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qandeel.core.designsystem.skin.QandeelTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QandeelAppThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a configuration change re-reads the wallpaper's palette`() {
        val wallpaper = WallpaperContext(ApplicationProvider.getApplicationContext())
        var configuration by mutableStateOf(Configuration())
        var primary by mutableStateOf(Color.Unspecified)

        wallpaper.seed = Color(0xFF1B5E20) // a green
        composeRule.setContent {
            CompositionLocalProvider(
                LocalContext provides wallpaper,
                LocalConfiguration provides configuration,
            ) {
                QandeelAppTheme(dynamicColor = true, tone = QandeelTone.LIGHT) {
                    val color = MaterialTheme.colorScheme.primary
                    SideEffect { primary = color }
                }
            }
        }
        composeRule.waitForIdle()
        val greenPrimary = primary

        wallpaper.seed = Color(0xFF4A148C) // a purple
        // A changed wallpaper rebuilds the palette resources; the configuration they arrive with
        // differs from the last (its resource sequence), and Compose forwards only configurations
        // that differ.
        configuration = Configuration(configuration).apply { screenWidthDp += 1 }
        composeRule.waitForIdle()

        assertNotEquals(greenPrimary, primary)
        assertEquals(dynamicLightColorScheme(wallpaper).primary, primary)
    }
}

/**
 * Serves every dynamic-color system resource as [seed], as a changed wallpaper would: Android
 * rebuilds those resources for the new palette and hands the app a new configuration.
 */
private class WallpaperContext(base: Context) : ContextWrapper(base) {
    private val wallpaperResources = WallpaperResources(base.resources)

    var seed: Color
        get() = wallpaperResources.seed
        set(value) {
            wallpaperResources.seed = value
        }

    override fun getResources(): Resources = wallpaperResources
}

private class WallpaperResources(base: Resources) :
    Resources(base.assets, base.displayMetrics, base.configuration) {
    var seed = Color.Black
    override fun getColor(id: Int, theme: Resources.Theme?): Int =
        if (id in dynamicColorIds) seed.toArgb() else super.getColor(id, theme)
}

/** The system's palette resources (system_accent1_0, system_neutral2_100, ...). */
private val dynamicColorIds: Set<Int> = android.R.color::class.java.fields
    .filter { it.name.startsWith("system_") }
    .mapTo(mutableSetOf()) { it.getInt(null) }
