package dev.sadakat.qit.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qit.testing.snapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class SettingsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun light() = composeRule.snapshot("settings_light") {
        Settings()
    }

    @Test
    fun dark() = composeRule.snapshot("settings_dark", darkTheme = true) {
        Settings()
    }
}

@Composable
private fun Settings() {
    ReadingSettingsContent(
        state = ReadingSettingsUiState(),
        showDynamicColor = true,
        onArabicTextSizeChange = {},
        onShowTranslationChange = {},
        onFollowAlongChange = {},
        onThemeModeChange = {},
        onDynamicColorChange = {},
    )
}
