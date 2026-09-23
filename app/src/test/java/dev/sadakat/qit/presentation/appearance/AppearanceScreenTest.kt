package dev.sadakat.qit.presentation.appearance

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.domain.model.UiStyle
import dev.sadakat.qit.ui.theme.QItAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppearanceScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var style: UiStyle? = null
    private var mode: ThemeMode? = null
    private var dynamicColor: Boolean? = null
    private var back = false

    private fun setContent(state: AppearanceUiState = AppearanceUiState(), showDynamicColor: Boolean = true) {
        composeRule.setContent {
            QItAppTheme {
                AppearanceScreen(
                    state = state,
                    showDynamicColor = showDynamicColor,
                    onStyleChange = { style = it },
                    onThemeModeChange = { mode = it },
                    onDynamicColorChange = { dynamicColor = it },
                    onBack = { back = true },
                )
            }
        }
    }

    @Test
    fun `each style is a radio choice and the stored one is selected`() {
        setContent(AppearanceUiState(style = UiStyle.GLASS))
        val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

        composeRule.onNodeWithTag("style_glass").assert(radio).assertIsSelected()
        composeRule.onNodeWithTag("style_mushaf").assert(radio).assert(isNotSelected())
    }

    @Test
    fun `picking a style applies it`() {
        setContent()
        composeRule.onNodeWithTag("style_expressive").performClick()
        assertEquals(UiStyle.EXPRESSIVE, style)
    }

    @Test
    fun `the page tones include sepia`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.theme_sepia)).performScrollTo().performClick()
        assertEquals(ThemeMode.SEPIA, mode)
    }

    @Test
    fun `wallpaper colors explain what they do on a sepia page`() {
        setContent(AppearanceUiState(themeMode = ThemeMode.SEPIA))
        composeRule.onNodeWithText(context.getString(R.string.wallpaper_colors_sepia_supporting)).performScrollTo()
        composeRule.onNodeWithText(context.getString(R.string.wallpaper_colors)).performClick()
        assertEquals(true, dynamicColor)
    }

    @Test
    fun `without Material You there is no wallpaper switch`() {
        setContent(showDynamicColor = false)
        composeRule.onNodeWithText(context.getString(R.string.wallpaper_colors)).assertDoesNotExist()
    }

    @Test
    fun `back goes back`() {
        setContent()
        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_back)).performClick()
        assertTrue(back)
    }

    @Test
    fun `system follows the device's light or dark`() {
        assertEquals(QItTone.DARK, ThemeMode.SYSTEM.tone(systemDark = true))
        assertEquals(QItTone.LIGHT, ThemeMode.SYSTEM.tone(systemDark = false))
        assertEquals(QItTone.SEPIA, ThemeMode.SEPIA.tone(systemDark = true))
    }
}
