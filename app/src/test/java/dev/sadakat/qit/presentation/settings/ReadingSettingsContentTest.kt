package dev.sadakat.qit.presentation.settings

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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sadakat.qit.R
import dev.sadakat.qit.core.domain.model.ArabicTextSize
import dev.sadakat.qit.core.domain.model.BanglaVoice
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.domain.model.WordByWord
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
    private var pickedVoice: BanglaVoice? = null
    private var wordByWord: WordByWord? = null
    private var pickedTheme: ThemeMode? = null
    private var openedAppearance = false
    private var openedAbout = false

    private fun setContent(prefs: ReadingPrefs = ReadingPrefs(), voice: BanglaVoice = BanglaVoice.DEFAULT) {
        composeRule.setContent {
            QItAppTheme {
                ReadingSettingsContent(
                    state = ReadingSettingsUiState(prefs = prefs, voice = voice),
                    onArabicTextSizeChange = { pickedSize = it },
                    onShowTranslationChange = { showTranslation = it },
                    onFollowAlongChange = { followAlong = it },
                    onBanglaVoiceChange = { pickedVoice = it },
                    onWordByWordChange = { wordByWord = it },
                    onThemeModeChange = { pickedTheme = it },
                    onOpenAppearance = { openedAppearance = true },
                    onOpenAbout = { openedAbout = true },
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
    fun `tapping a bangla voice row anywhere picks it`() {
        setContent()

        composeRule.onNodeWithText(context.getString(R.string.bangla_voice_toha)).performScrollTo().performClick()

        assertEquals(BanglaVoice.SAYED_ISMAT_TOHA, pickedVoice)
    }

    @Test
    fun `the stored bangla voice is the checked row`() {
        setContent(voice = BanglaVoice.SHAREEF_BAEZEED_MAHMOOD)

        composeRule.onNodeWithText(context.getString(R.string.bangla_voice_baezeed))
            .performScrollTo()
            .assert(isSelected())
        composeRule.onNodeWithText(context.getString(R.string.bangla_voice_islamic_foundation))
            .assert(isNotSelected())
    }

    @Test
    fun `the word-by-word segments report the picked language`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.word_by_word_bangla)).performScrollTo().performClick()
        assertEquals(WordByWord.BANGLA, wordByWord)
    }

    @Test
    fun `word by word is off until a language is picked`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.word_by_word_off)).assert(isSelected())
    }

    @Test
    fun `the stored word-by-word language is selected`() {
        setContent(prefs = ReadingPrefs(wordByWord = WordByWord.ENGLISH))
        composeRule.onNodeWithText(context.getString(R.string.word_by_word_english)).assert(isSelected())
        composeRule.onNodeWithText(context.getString(R.string.word_by_word_off)).assert(isNotSelected())
    }

    @Test
    fun `the theme segments report the picked mode`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.theme_dark)).performScrollTo().performClick()
        assertEquals(ThemeMode.DARK, pickedTheme)
    }

    @Test
    fun `the current theme is selected`() {
        setContent(prefs = ReadingPrefs(themeMode = ThemeMode.DARK))
        composeRule.onNodeWithText(context.getString(R.string.theme_dark)).assert(isSelected())
        composeRule.onNodeWithText(context.getString(R.string.theme_light)).assert(isNotSelected())
    }

    @Test
    fun `sepia is one of the page tones`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.theme_sepia)).performScrollTo().performClick()
        assertEquals(ThemeMode.SEPIA, pickedTheme)
    }

    @Test
    fun `the sheet leads on to Appearance`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.appearance_more)).performScrollTo().performClick()
        assertEquals(true, openedAppearance)
    }

    @Test
    fun `the sheet leads on to About`() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.about_open)).performScrollTo().performClick()
        assertEquals(true, openedAbout)
    }

    @Test
    fun `the size slider says the size's name, not its position`() {
        setContent(prefs = ReadingPrefs(arabicTextSize = ArabicTextSize.LARGE))
        composeRule.onNode(hasSettableProgress()).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.arabic_size_name_large),
            ),
        )
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
