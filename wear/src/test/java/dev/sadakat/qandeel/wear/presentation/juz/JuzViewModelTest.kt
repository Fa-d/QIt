package dev.sadakat.qandeel.wear.presentation.juz

import app.cash.turbine.test
import dev.sadakat.qandeel.core.domain.model.QuranMeta
import dev.sadakat.qandeel.core.testing.FakeQuranText
import dev.sadakat.qandeel.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JuzViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val text = FakeQuranText()

    @Test
    fun `rows list the 30 juz with their start and surah name`() = runTest {
        JuzViewModel(text).uiState.test {
            val rows = awaitItem().let { if (it.rows.isEmpty()) awaitItem().rows else it.rows }

            assertEquals(QuranMeta.JUZ_COUNT, rows.size)
            val fifteenth = rows.single { it.juz == 15 }
            assertEquals(17, fifteenth.start.surah)
            assertEquals(1, fifteenth.start.ayah)
            assertEquals("Surah 17", fifteenth.surahName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `surah names come from the text source and vanish when it fails`() = runTest {
        val viewModel = JuzViewModel(text)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(mutableListOf()) }
        runCurrent()
        assertEquals("Al-Faatiha", viewModel.uiState.value.rows.single { it.juz == 1 }.surahName)

        text.failure = IllegalStateException("no assets")
        val fresh = JuzViewModel(text)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { fresh.uiState.toList(mutableListOf()) }
        runCurrent()
        assertNull(fresh.uiState.value.rows.single { it.juz == 1 }.surahName)
    }
}
