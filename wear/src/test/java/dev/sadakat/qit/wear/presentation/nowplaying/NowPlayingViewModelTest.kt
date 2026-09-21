package dev.sadakat.qit.wear.presentation.nowplaying

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.testing.FakeQuranPlayer
import dev.sadakat.qit.core.testing.FakeQuranText
import dev.sadakat.qit.core.testing.MainDispatcherRule
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
class NowPlayingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val text = FakeQuranText()
    private val player = FakeQuranPlayer()

    private fun viewModel() = NowPlayingViewModel(text, player)

    /**
     * Waits for the first upstream result. Under the unconfined test dispatcher the initial
     * [NowPlayingViewModel.UiState] may already be conflated away with it, so skip it if seen.
     */
    private suspend fun TurbineTestContext<NowPlayingViewModel.UiState>.awaitLoaded() =
        awaitItem().let { if (it.surahNumber == null) awaitItem() else it }

    @Test
    fun `shows surah name, position and the Arabic text of the current ayah`() = runTest {
        player.play(2, 255, RecitationMode.ARABIC_BANGLA)

        viewModel().uiState.test {
            val state = awaitLoaded()

            assertEquals(2, state.surahNumber)
            assertEquals("Al-Baqara", state.surahName)
            assertEquals(255, state.ayah)
            assertEquals("آية 2:255", state.ayahText)
            assertEquals(true, state.isPlaying)
            assertEquals(false, state.isBuffering)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `basmala position carries no ayah text`() = runTest {
        player.play(2, 0, RecitationMode.ARABIC_BANGLA)

        viewModel().uiState.test {
            val state = awaitLoaded()

            assertEquals(2, state.surahNumber)
            assertEquals("Al-Baqara", state.surahName)
            assertEquals(0, state.ayah)
            assertNull(state.ayahText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exposes the playback error alongside the position`() = runTest {
        player.play(112, 3, RecitationMode.ARABIC_ONLY)
        player.error.value = "No network"

        viewModel().uiState.test {
            assertEquals("No network", awaitLoaded().error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `nothing queued yields an empty state`() = runTest {
        val viewModel = viewModel()
        // The loaded state equals the initial one here, so drive the flow from backgroundScope and
        // read .value instead of waiting for a second stream item that will never come.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(mutableListOf()) }
        runCurrent()

        assertEquals(NowPlayingViewModel.UiState(), viewModel.uiState.value)
    }

    @Test
    fun `controls drive the player`() = runTest {
        player.play(1, 2, RecitationMode.ARABIC_ONLY)
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(2, awaitLoaded().ayah)

            viewModel.nextAyah()
            assertEquals(3, awaitItem().ayah)

            viewModel.previousAyah()
            assertEquals(2, awaitItem().ayah)

            viewModel.togglePlayPause()
            assertEquals(false, awaitItem().isPlaying)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
