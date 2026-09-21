package dev.sadakat.qit.presentation.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sadakat.qit.R
import dev.sadakat.qit.core.domain.model.ArabicTextSize
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.ui.theme.QItAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadingSettingsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var pickedSize: ArabicTextSize? = null
    private var showTranslation: Boolean? = null
    private var followAlong: Boolean? = null
    private var pickedTheme: ThemeMode? = null
    private var dynamicColor: Boolean? = null

    private fun setContent(prefs: ReadingPrefs = ReadingPrefs(), showDynamicColor: Boolean = true) {
        composeRule.setContent {
            QItAppTheme {
                ReadingSettingsContent(
                    state = ReadingSettingsUiState(prefs = prefs),
                    showDynamicColor = showDynamicColor,
                    onArabicTextSizeChange = { pickedSize = it },
                    onShowTranslationChange = { showTranslation = it },
                    onFollowAlongChange = { followAlong = it },
                    onThemeModeChange = { pickedTheme = it },
                    onDynamicColorChange = { dynamicColor = it },
                )
            }
        }
    }

    @Test
    fun `the slider picks the arabic text size`() {
        setContent()
        composeRule.onNode(hasSettableProgress())
            .performSemanticsAction(SemanticsActions.SetProgress) { it(3f) }
        assertEquals(ArabicTextSize.XLARGE, pickedSize)
    }

    @Test
    fun `tapping a row anywhere toggles its switch`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.show_translation)).performClick()
        assertEquals(false, showTranslation)

        composeRule.onNodeWithText(context.getString(R.string.follow_along)).performClick()
        assertEquals(false, followAlong)
    }

    @Test
    fun `the switches show the stored state`() {
        setContent(prefs = ReadingPrefs(showTranslation = false, followAlong = false))
        composeRule.onNodeWithText(context.getString(R.string.show_translation))
            .assert(isSwitch(false))
        composeRule.onNodeWithText(context.getString(R.string.follow_along))
            .assert(isSwitch(false))
    }

    @Test
    fun `the theme segments report the picked mode`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.theme_dark)).performClick()
        assertEquals(ThemeMode.DARK, pickedTheme)
    }

    @Test
    fun `the current theme is selected`() {
        setContent(prefs = ReadingPrefs(themeMode = ThemeMode.DARK))
        composeRule.onNodeWithText(context.getString(R.string.theme_dark)).assert(isSelected())
        composeRule.onNodeWithText(context.getString(R.string.theme_light)).assert(isNotSelected())
    }

    @Test
    fun `wallpaper colors appear only where the device supports them`() {
        // One content per test; the flag is snapshot state the content reads live.
        val showDynamicColor = mutableStateOf(false)
        composeRule.setContent {
            QItAppTheme {
                ReadingSettingsContent(
                    state = ReadingSettingsUiState(),
                    showDynamicColor = showDynamicColor.value,
                    onArabicTextSizeChange = {},
                    onShowTranslationChange = {},
                    onFollowAlongChange = {},
                    onThemeModeChange = {},
                    onDynamicColorChange = { dynamicColor = it },
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.wallpaper_colors)).assertDoesNotExist()

        composeRule.runOnIdle { showDynamicColor.value = true }
        composeRule.onNodeWithText(context.getString(R.string.wallpaper_colors)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.wallpaper_colors)).performClick()
        assertEquals(true, dynamicColor)
    }

    @Test
    fun `the arabic preview is shown under the slider`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.arabic_size_preview)).assertIsDisplayed()
    }

    private fun hasSettableProgress() = SemanticsMatcher("has a settable progress") {
        SemanticsActions.SetProgress in it.config
    }

    private fun isSwitch(checked: Boolean) = SemanticsMatcher("switch is $checked") {
        it.config.getOrNull(SemanticsProperties.ToggleableState) ==
            if (checked) ToggleableState.On else ToggleableState.Off
    }

    private fun isNotSelected() = isSelected().not()
}
