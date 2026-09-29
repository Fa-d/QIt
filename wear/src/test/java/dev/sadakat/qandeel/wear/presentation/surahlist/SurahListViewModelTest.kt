package dev.sadakat.qandeel.wear.presentation.surahlist

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.model.Track
import dev.sadakat.qandeel.core.domain.repository.SurahDownloadState
import dev.sadakat.qandeel.core.testing.FakeQuranSettings
import dev.sadakat.qandeel.core.testing.FakeQuranText
import dev.sadakat.qandeel.core.testing.FakeSurahDownloads
import dev.sadakat.qandeel.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SurahListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val text = FakeQuranText()
    private val settings = FakeQuranSettings()
    private val downloads = FakeSurahDownloads()

    private fun viewModel(downloadedOnly: Boolean) =
        SurahListViewModel(SavedStateHandle(mapOf("downloaded" to downloadedOnly)), text, settings, downloads)

    private suspend fun TurbineTestContext<WearSurahListUiState>.awaitLoaded() =
        awaitItem().let { if (it.rows.isEmpty()) awaitItem() else it }

    @Test
    fun `rows list every surah with the download state of the current mode tracks`() = runTest {
        downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloaded)
        downloads.setState(2, Track.BANGLA, SurahDownloadState.Downloading(3, 286))
        downloads.setState(112, Track.BANGLA, SurahDownloadState.Failed(1, 4))

        viewModel(downloadedOnly = false).uiState.test {
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

        val viewModel = viewModel(downloadedOnly = false)
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
        val viewModel = viewModel(downloadedOnly = false)
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
    fun `the downloaded variant lists only fully downloaded surahs`() = runTest {
        downloads.setState(112, Track.ARABIC, SurahDownloadState.Downloaded)
        downloads.setState(112, Track.BANGLA, SurahDownloadState.Downloaded)
        downloads.setState(2, Track.BANGLA, SurahDownloadState.Downloading(3, 286))

        viewModel(downloadedOnly = true).uiState.test {
            val state = awaitLoaded()

            assertTrue(state.downloadedOnly)
            assertEquals(listOf(112), state.rows.map { it.surah.number })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failing text source loads an empty list`() = runTest {
        text.failure = IllegalStateException("no assets")
        val viewModel = viewModel(downloadedOnly = false)
        // The loaded state equals the initial one here, so drive the flow from backgroundScope and
        // read .value instead of waiting for a second stream item that will never come.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(mutableListOf()) }
        runCurrent()

        assertTrue(viewModel.uiState.value.rows.isEmpty())
    }
}
