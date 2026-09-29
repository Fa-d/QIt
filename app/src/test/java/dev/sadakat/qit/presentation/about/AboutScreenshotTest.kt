package dev.sadakat.qit.presentation.about

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.testing.snapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AboutScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun light() = composeRule.snapshot("about_light") { AboutScreen(versionName = "1.0.0", onBack = {}) }

    @Test
    fun dark() = composeRule.snapshot("about_dark", tone = QItTone.DARK) {
        AboutScreen(versionName = "1.0.0", onBack = {})
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, fontScale = 1.3f)
    fun largeText() = composeRule.snapshot("about_large_text") { AboutScreen(versionName = "1.0.0", onBack = {}) }
}
