package dev.sadakat.qit.wear.presentation.surah

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.testing.FakeQuranPlayer
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.FakeQuranText
import dev.sadakat.qit.core.testing.FakeSurahDownloads
import dev.sadakat.qit.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearSurahViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val text = FakeQuranText()
    private val settings = FakeQuranSettings()
    private val downloads = FakeSurahDownloads()
    private val player = FakeQuranPlayer()

    private fun viewModel(args: Map<String, Any?>) =
        WearSurahViewModel(SavedStateHandle(args), text, settings, downloads, player)

    /**
     * Waits for the first upstream result. Under the unconfined test dispatcher the initial
     * [WearSurahUiState] may already be conflated away with it, so skip it if seen.
     */
    private suspend fun TurbineTestContext<WearSurahUiState>.awaitLoaded() =
        awaitItem().let { if (it.surah == null) awaitItem() else it }

    @Test
    fun `loads the surah and the download state of the current mode tracks`() = runTest {
        downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloaded)
        downloads.setState(2, Track.BANGLA, SurahDownloadState.Downloading(1, 286))

        viewModel(mapOf("number" to 2)).uiState.test {
            val state = awaitLoaded()

            assertEquals("Al-Baqara", state.surah?.nameEnglish)
            assertEquals(286, state.surah?.ayahCount)
            assertEquals(RecitationMode.ARABIC_BANGLA, state.mode)
            // Arabic fully downloaded + Bangla 1/286 (mode ARABIC_BANGLA) => still downloading.
            assertEquals(SurahDownloadState.Downloading(1, 286), state.download)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unknown surah number yields a not-found state and refuses actions`() = runTest {
        val viewModel = viewModel(mapOf("number" to 200))
        // The loaded state equals the initial one here, so drive the flow from backgroundScope and
        // read .value instead of waiting for a second stream item that will never come.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(mutableListOf()) }
        runCurrent()

        assertNull(viewModel.uiState.value.surah)

        viewModel.play()
        viewModel.download()
        viewModel.remove()
        assertTrue(player.playCalls.isEmpty())
        assertTrue(downloads.downloadRequests.isEmpty())
        assertTrue(downloads.removeRequests.isEmpty())
    }

    @Test
    fun `play starts from the basmala for surahs that have one`() = runTest {
        val viewModel = viewModel(mapOf("number" to 2))
        viewModel.uiState.test {
            awaitLoaded()

            viewModel.play()
            assertEquals(FakeQuranPlayer.PlayCall(2, 0, RecitationMode.ARABIC_BANGLA), player.playCalls.single())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `play starts from ayah 1 for Al-Fatiha and At-Tawbah`() = runTest {
        val fatiha = viewModel(mapOf("number" to 1))
        fatiha.uiState.test {
            awaitLoaded()
            fatiha.play()
            cancelAndIgnoreRemainingEvents()
        }
        val tawba = viewModel(mapOf("number" to 9))
        tawba.uiState.test {
            awaitLoaded()
            tawba.play()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(
            listOf(
                FakeQuranPlayer.PlayCall(1, 1, RecitationMode.ARABIC_BANGLA),
                FakeQuranPlayer.PlayCall(9, 1, RecitationMode.ARABIC_BANGLA),
            ),
            player.playCalls,
        )
    }

    @Test
    fun `play starts from the given juz start when opened from the juz list`() = runTest {
        val viewModel = viewModel(mapOf("number" to 17, "from" to 1))
        viewModel.uiState.test {
            awaitLoaded()

            viewModel.play()
            assertEquals(FakeQuranPlayer.PlayCall(17, 1, RecitationMode.ARABIC_BANGLA), player.playCalls.single())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `play, download and remove act on the current mode tracks`() = runTest {
        settings.setMode(RecitationMode.ARABIC_ENGLISH)
        val viewModel = viewModel(mapOf("number" to 2))
        viewModel.uiState.test {
            awaitLoaded()

            viewModel.play()
            assertEquals(FakeQuranPlayer.PlayCall(2, 0, RecitationMode.ARABIC_ENGLISH), player.playCalls.single())

            viewModel.download()
            assertEquals(listOf(2 to listOf(Track.ARABIC, Track.ENGLISH)), downloads.downloadRequests)

            viewModel.remove()
            assertEquals(listOf(2 to listOf(Track.ARABIC, Track.ENGLISH)), downloads.removeRequests)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
