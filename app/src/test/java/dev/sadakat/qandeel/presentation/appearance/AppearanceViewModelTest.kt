package dev.sadakat.qandeel.presentation.appearance

import app.cash.turbine.test
import dev.sadakat.qandeel.core.domain.model.ReadingPrefs
import dev.sadakat.qandeel.core.domain.model.ThemeMode
import dev.sadakat.qandeel.core.domain.model.UiStyle
import dev.sadakat.qandeel.core.testing.FakeQuranSettings
import dev.sadakat.qandeel.core.testing.MainDispatcherRule
import dev.sadakat.qandeel.presentation.awaitWhere
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppearanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeQuranSettings()

    @Test
    fun `the state mirrors the stored look`() = runTest {
        settings.readingPrefs.value =
            ReadingPrefs(uiStyle = UiStyle.GLASS, themeMode = ThemeMode.SEPIA, dynamicColor = true)
        AppearanceViewModel(settings).uiState.test {
            assertEquals(
                AppearanceUiState(UiStyle.GLASS, ThemeMode.SEPIA, dynamicColor = true),
                awaitWhere { it.style == UiStyle.GLASS },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `every choice is stored and leaves the reading prefs alone`() = runTest {
        settings.readingPrefs.value = ReadingPrefs(showTranslation = false)
        val viewModel = AppearanceViewModel(settings)
        viewModel.uiState.test {
            viewModel.setStyle(UiStyle.EXPRESSIVE)
            awaitWhere { it.style == UiStyle.EXPRESSIVE }
            viewModel.setThemeMode(ThemeMode.DARK)
            awaitWhere { it.themeMode == ThemeMode.DARK }
            viewModel.setDynamicColor(true)
            awaitWhere { it.dynamicColor }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(
            ReadingPrefs(
                showTranslation = false,
                uiStyle = UiStyle.EXPRESSIVE,
                themeMode = ThemeMode.DARK,
                dynamicColor = true,
            ),
            settings.readingPrefs.value,
        )
    }
}
