package dev.sadakat.qit.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.core.domain.model.ArabicTextSize
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.ThemeMode
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
    fun dark() = composeRule.snapshot("settings_dark", tone = QItTone.DARK) {
        Settings()
    }

    @Test
    fun sepia() = composeRule.snapshot("settings_sepia", tone = QItTone.SEPIA) {
        Settings(ReadingPrefs(themeMode = ThemeMode.SEPIA))
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, fontScale = 1.3f)
    fun largeText() = composeRule.snapshot("settings_large_text", arabicScale = ArabicTextSize.XXLARGE.scale) {
        Settings(ReadingPrefs(arabicTextSize = ArabicTextSize.XXLARGE))
    }
}

@Composable
private fun Settings(prefs: ReadingPrefs = ReadingPrefs()) {
    ReadingSettingsContent(
        state = ReadingSettingsUiState(prefs),
        onArabicTextSizeChange = {},
        onShowTranslationChange = {},
        onFollowAlongChange = {},
        onBanglaVoiceChange = {},
        onWordByWordChange = {},
        onThemeModeChange = {},
        onOpenAppearance = {},
        onOpenAbout = {},
    )
}
