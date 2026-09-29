package dev.sadakat.qandeel.wear.presentation.mode

import app.cash.turbine.test
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.testing.FakeQuranSettings
import dev.sadakat.qandeel.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ModeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeQuranSettings()

    @Test
    fun `exposes the persisted mode`() = runTest {
        settings.setMode(RecitationMode.ARABIC_ENGLISH)

        ModeViewModel(settings).uiState.test {
            assertEquals(RecitationMode.ARABIC_ENGLISH, awaitItem().mode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting a mode persists it`() = runTest {
        val viewModel = ModeViewModel(settings)
        viewModel.uiState.test {
            assertEquals(RecitationMode.ARABIC_BANGLA, awaitItem().mode)

            viewModel.select(RecitationMode.ARABIC_ONLY)
            assertEquals(RecitationMode.ARABIC_ONLY, awaitItem().mode)
            assertEquals(RecitationMode.ARABIC_ONLY, settings.mode.value)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
