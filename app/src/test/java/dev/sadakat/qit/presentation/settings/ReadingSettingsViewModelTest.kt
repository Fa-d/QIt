package dev.sadakat.qit.presentation.settings

import app.cash.turbine.test
import dev.sadakat.qit.core.domain.model.ArabicTextSize
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.domain.model.WordByWord
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.MainDispatcherRule
import dev.sadakat.qit.presentation.awaitWhere
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReadingSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeQuranSettings()

    private fun viewModel() = ReadingSettingsViewModel(settings)

    @Test
    fun `the state mirrors the stored reading prefs`() = runTest {
        settings.readingPrefs.value = ReadingPrefs(themeMode = ThemeMode.DARK, dynamicColor = true)
        viewModel().uiState.test {
            val state = awaitWhere { it.prefs.themeMode == ThemeMode.DARK }
            assertEquals(ThemeMode.DARK, state.prefs.themeMode)
            assertEquals(true, state.prefs.dynamicColor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `every action updates its own pref`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.prefs == ReadingPrefs() }

            viewModel.setArabicTextSize(ArabicTextSize.XXLARGE)
            assertEquals(
                ArabicTextSize.XXLARGE,
                awaitWhere {
                    it.prefs.arabicTextSize == ArabicTextSize.XXLARGE
                }.prefs.arabicTextSize,
            )

            viewModel.setShowTranslation(false)
            assertEquals(false, awaitWhere { !it.prefs.showTranslation }.prefs.showTranslation)

            viewModel.setFollowAlong(false)
            assertEquals(false, awaitWhere { !it.prefs.followAlong }.prefs.followAlong)

            viewModel.setWordByWord(WordByWord.BANGLA)
            assertEquals(
                WordByWord.BANGLA,
                awaitWhere { it.prefs.wordByWord == WordByWord.BANGLA }.prefs.wordByWord,
            )

            viewModel.setThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitWhere { it.prefs.themeMode == ThemeMode.LIGHT }.prefs.themeMode)

            viewModel.setDynamicColor(true)
            assertEquals(true, awaitWhere { it.prefs.dynamicColor }.prefs.dynamicColor)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
