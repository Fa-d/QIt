package dev.sadakat.qit.presentation

import app.cash.turbine.test
import dev.sadakat.qit.core.domain.model.ArabicTextSize
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `nothing is ready before the settings are read`() {
        assertFalse(AppViewModel(FakeQuranSettings()).uiState.value.isReady)
    }

    @Test
    fun `the look follows the reading settings`() = runTest {
        val settings = FakeQuranSettings(
            readingPrefs = ReadingPrefs(
                themeMode = ThemeMode.DARK,
                dynamicColor = true,
                arabicTextSize = ArabicTextSize.LARGE,
            ),
        )
        AppViewModel(settings).uiState.test {
            assertEquals(
                AppUiState(isReady = true, ThemeMode.DARK, dynamicColor = true, arabicTextSize = ArabicTextSize.LARGE),
                awaitWhere { it.isReady },
            )
            settings.updateReadingPrefs { it.copy(themeMode = ThemeMode.LIGHT) }
            assertEquals(ThemeMode.LIGHT, awaitWhere { it.themeMode == ThemeMode.LIGHT }.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
