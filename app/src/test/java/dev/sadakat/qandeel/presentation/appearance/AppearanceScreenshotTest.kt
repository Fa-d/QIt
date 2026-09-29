package dev.sadakat.qandeel.presentation.appearance

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qandeel.core.designsystem.skin.QandeelStyle
import dev.sadakat.qandeel.core.designsystem.skin.QandeelTone
import dev.sadakat.qandeel.core.domain.model.ThemeMode
import dev.sadakat.qandeel.core.domain.model.UiStyle
import dev.sadakat.qandeel.testing.snapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AppearanceScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun light() = composeRule.snapshot("appearance_light") { Appearance(AppearanceUiState()) }

    @Test
    fun dark() = composeRule.snapshot("appearance_dark", tone = QandeelTone.DARK) {
        Appearance(AppearanceUiState(themeMode = ThemeMode.DARK))
    }

    @Test
    fun glassSepia() =
        composeRule.snapshot("appearance_glass_sepia", style = QandeelStyle.GLASS, tone = QandeelTone.SEPIA) {
            Appearance(AppearanceUiState(style = UiStyle.GLASS, themeMode = ThemeMode.SEPIA))
        }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, fontScale = 1.3f)
    fun largeText() = composeRule.snapshot("appearance_large_text") { Appearance(AppearanceUiState()) }
}

@Composable
private fun Appearance(state: AppearanceUiState) {
    AppearanceScreen(
        state = state,
        showDynamicColor = true,
        onStyleChange = {},
        onThemeModeChange = {},
        onDynamicColorChange = {},
        onBack = {},
    )
}
