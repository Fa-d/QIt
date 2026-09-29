package dev.sadakat.qandeel.wear.presentation.nowplaying

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.player.PlaybackError
import dev.sadakat.qandeel.core.testing.FakeQuranPlayer
import dev.sadakat.qandeel.core.testing.FakeQuranText
import dev.sadakat.qandeel.core.testing.MainDispatcherRule
import dev.sadakat.qandeel.wear.audio.FakeStreamVolume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
    private val volume = FakeStreamVolume(level = 0.5f)

    private fun viewModel() = NowPlayingViewModel(text, player, volume)

    /**
     * Waits for the first upstream result. Under the unconfined test dispatcher the initial
     * [WearNowPlayingUiState] may already be conflated away with it, so skip it if seen.
     */
    private suspend fun TurbineTestContext<WearNowPlayingUiState>.awaitLoaded() =
        awaitItem().let { if (it.surahNumber == null) awaitItem() else it }

    @Test
    fun `shows surah name, position, the Arabic text, the translation and the progress`() = runTest {
        player.play(2, 255, RecitationMode.ARABIC_BANGLA)

        viewModel().uiState.test {
            val state = awaitLoaded()

            assertEquals(2, state.surahNumber)
            assertEquals("Al-Baqara", state.surahName)
            assertEquals(255, state.ayah)
            assertEquals("آية 2:255", state.ayahText)
            assertEquals("বাংলা 2:255", state.translation)
            assertEquals(true, state.isPlaying)
            assertEquals(false, state.isBuffering)
            // Ayah 255 of 286, by ayah.
            assertEquals(255f / 286f, state.progress, 0.0001f)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `arabic-only mode carries no translation`() = runTest {
        player.play(112, 3, RecitationMode.ARABIC_ONLY)

        viewModel().uiState.test {
            assertNull(awaitLoaded().translation)
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
            assertEquals(0f, state.progress, 0f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exposes the playback error alongside the position`() = runTest {
        player.play(112, 3, RecitationMode.ARABIC_ONLY)
        player.error.value = PlaybackError.NETWORK

        viewModel().uiState.test {
            assertEquals(PlaybackError.NETWORK, awaitLoaded().error)
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

        assertEquals(WearNowPlayingUiState(volume = 0.5f), viewModel.uiState.value)
    }

    @Test
    fun `the volume level flows into the state and adjustVolume drives the stream`() = runTest {
        // Nothing is queued here, so the loaded state differs from the initial one only by the
        // volume — read .value with an eager collector instead of a second stream item.
        val viewModel = collected()

        assertEquals(0.5f, viewModel.uiState.value.volume, 0.0001f)

        viewModel.adjustVolume(2)
        assertEquals(listOf(2), volume.steps)
        assertEquals(0.7f, viewModel.uiState.value.volume, 0.0001f)
    }

    /** A [NowPlayingViewModel] whose state is being collected eagerly, so `.value` is current. */
    private fun TestScope.collected(): NowPlayingViewModel {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(mutableListOf()) }
        runCurrent()
        return viewModel
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
