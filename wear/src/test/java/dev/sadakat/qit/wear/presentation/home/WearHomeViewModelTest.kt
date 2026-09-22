package dev.sadakat.qit.wear.presentation.home

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.testing.FakeQuranPlayer
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.FakeQuranText
import dev.sadakat.qit.core.testing.FakeSurahDownloads
import dev.sadakat.qit.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val text = FakeQuranText()
    private val settings = FakeQuranSettings()
    private val downloads = FakeSurahDownloads()
    private val player = FakeQuranPlayer()

    private fun viewModel() = WearHomeViewModel(text, settings, downloads, player)

    /**
     * Waits for the first upstream result. Under the unconfined test dispatcher the initial
     * [WearHomeUiState] may already be conflated away with it, so skip it if seen.
     */
    private suspend fun TurbineTestContext<WearHomeUiState>.awaitLoaded() =
        awaitItem().let { if (!it.loaded) awaitItem() else it }

    @Test
    fun `counts downloaded surahs of the current mode and hides the row at zero`() = runTest {
        downloads.setState(112, Track.ARABIC, SurahDownloadState.Downloaded)
        downloads.setState(114, Track.ARABIC, SurahDownloadState.Downloaded)
        downloads.setState(114, Track.BANGLA, SurahDownloadState.Downloaded)

        viewModel().uiState.test {
            val state = awaitLoaded()

            // 112 needs Bangla too in ARABIC_BANGLA; only 114 is fully downloaded.
            assertEquals(1, state.downloadedCount)
            assertTrue(state.loaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `download count follows the selected mode`() = runTest {
        downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloaded)
        settings.setMode(RecitationMode.ARABIC_ONLY)

        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(1, awaitLoaded().downloadedCount)

            // Arabic + Bangla: the Bangla track was never requested, so 2 is not fully offline.
            settings.setMode(RecitationMode.ARABIC_BANGLA)
            assertEquals(0, awaitItem().downloadedCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `something queued means Now playing, and no continue position`() = runTest {
        settings.lastPosition.value = LastPosition(AyahRef(2, 255), RecitationMode.ARABIC_BANGLA)
        player.play(2, 255, RecitationMode.ARABIC_BANGLA)

        viewModel().uiState.test {
            val state = awaitLoaded()

            assertTrue(state.isQueued)
            assertNull(state.continuePosition)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `nothing queued shows the last saved position to continue from`() = runTest {
        settings.lastPosition.value = LastPosition(AyahRef(18, 23), RecitationMode.ARABIC_ONLY)

        viewModel().uiState.test {
            val state = awaitLoaded()

            assertFalse(state.isQueued)
            assertEquals("18:23", state.continuePosition)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no saved position and nothing queued means no edge button`() = runTest {
        viewModel().uiState.test {
            val state = awaitLoaded()

            assertFalse(state.isQueued)
            assertNull(state.continuePosition)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `continuePlaying restores the last saved position`() = runTest {
        settings.lastPosition.value = LastPosition(AyahRef(112, 3), RecitationMode.ARABIC_ONLY)
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitLoaded()

            viewModel.continuePlaying()
            assertEquals(1, player.restoreCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the hub exposes the current mode`() = runTest {
        settings.setMode(RecitationMode.ARABIC_ENGLISH)

        viewModel().uiState.test {
            assertEquals(RecitationMode.ARABIC_ENGLISH, awaitLoaded().mode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failing text source still loads an empty hub`() = runTest {
        text.failure = IllegalStateException("no assets")

        // The loaded state equals the initial one here, so read .value with an eager collector
        // instead of waiting for a second stream item that will never come.
        val viewModel = collected()

        assertTrue(!viewModel.uiState.value.loaded)
    }

    /** A [WearHomeViewModel] whose state is being collected eagerly, so `.value` is current. */
    private fun TestScope.collected(): WearHomeViewModel {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(mutableListOf()) }
        runCurrent()
        return viewModel
    }
}
