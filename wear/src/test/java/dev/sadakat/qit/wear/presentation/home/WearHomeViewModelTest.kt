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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

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
     * [WearHomeViewModel.UiState] may already be conflated away with it, so skip it if seen.
     */
    private suspend fun TurbineTestContext<WearHomeViewModel.UiState>.awaitLoaded() =
        awaitItem().let { if (it.rows.isEmpty()) awaitItem() else it }

    @Test
    fun `rows list every surah with the download state of the current mode tracks`() = runTest {
        downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloaded)
        downloads.setState(2, Track.BANGLA, SurahDownloadState.Downloading(3, 286))
        downloads.setState(112, Track.BANGLA, SurahDownloadState.Failed(1, 4))

        viewModel().uiState.test {
            val state = awaitLoaded()

            assertEquals(114, state.rows.size)
            assertEquals(SurahDownloadState.NotDownloaded, state.rows.first { it.surah.number == 1 }.download)
            // Arabic downloaded + Bangla downloading (mode ARABIC_BANGLA) => still downloading.
            assertEquals(
                SurahDownloadState.Downloading(3, 286),
                state.rows.first { it.surah.number == 2 }.download,
            )
            // Arabic never requested + Bangla failed => failed.
            assertEquals(
                SurahDownloadState.Failed(1, 4),
                state.rows.first { it.surah.number == 112 }.download,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `download state follows the selected mode`() = runTest {
        downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloaded)
        settings.setMode(RecitationMode.ARABIC_ONLY)

        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(SurahDownloadState.Downloaded, awaitLoaded().rows.first { it.surah.number == 2 }.download)

            // Switching to Arabic + Bangla: the Bangla track was never requested, so 2 is not fully offline.
            settings.setMode(RecitationMode.ARABIC_BANGLA)
            assertEquals(
                SurahDownloadState.NotDownloaded,
                awaitItem().rows.first { it.surah.number == 2 }.download,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `download progress updates the rows`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitLoaded()

            downloads.setState(114, Track.ARABIC, SurahDownloadState.Downloading(1, 6))
            assertEquals(
                SurahDownloadState.Downloading(1, 6),
                awaitItem().rows.single { it.surah.number == 114 }.download,
            )

            downloads.setState(114, Track.ARABIC, SurahDownloadState.Downloaded)
            // Arabic downloaded but Bangla (of the current mode) not requested => not offline yet.
            assertEquals(
                SurahDownloadState.NotDownloaded,
                awaitItem().rows.single { it.surah.number == 114 }.download,
            )

            downloads.setState(114, Track.BANGLA, SurahDownloadState.Downloaded)
            assertEquals(
                SurahDownloadState.Downloaded,
                awaitItem().rows.single { it.surah.number == 114 }.download,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cycleMode steps through the three modes in order and persists`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(RecitationMode.ARABIC_BANGLA, awaitLoaded().mode)

            viewModel.cycleMode()
            assertEquals(RecitationMode.ARABIC_ONLY, awaitItem().mode)
            assertEquals(RecitationMode.ARABIC_ONLY, settings.mode.value)

            viewModel.cycleMode()
            assertEquals(RecitationMode.ARABIC_ENGLISH, awaitItem().mode)

            viewModel.cycleMode()
            assertEquals(RecitationMode.ARABIC_BANGLA, awaitItem().mode)

            viewModel.cycleMode()
            assertEquals(RecitationMode.ARABIC_ONLY, awaitItem().mode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `now playing chip shows the queued position and hides the continue chip`() = runTest {
        settings.lastPosition.value = LastPosition(AyahRef(2, 255), RecitationMode.ARABIC_BANGLA)
        player.play(2, 255, RecitationMode.ARABIC_BANGLA)

        viewModel().uiState.test {
            val state = awaitLoaded()

            assertEquals(WearHomeViewModel.NowPlayingChip("Al-Baqara", "2:255"), state.nowPlayingChip)
            assertNull(state.continueChip)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `continue chip shows the last position when nothing is queued`() = runTest {
        settings.lastPosition.value = LastPosition(AyahRef(112, 3), RecitationMode.ARABIC_ONLY)

        viewModel().uiState.test {
            val state = awaitLoaded()

            assertNull(state.nowPlayingChip)
            assertEquals(WearHomeViewModel.ContinueChip("Al-Ikhlaas", "112:3"), state.continueChip)
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
}
