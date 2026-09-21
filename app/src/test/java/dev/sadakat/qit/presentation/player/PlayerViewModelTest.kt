package dev.sadakat.qit.presentation.player

import app.cash.turbine.test
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.testing.FakeQuranPlayer
import dev.sadakat.qit.core.testing.FakeQuranText
import dev.sadakat.qit.core.testing.MainDispatcherRule
import dev.sadakat.qit.presentation.awaitWhere
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val quranText = FakeQuranText()
    private val player = FakeQuranPlayer()

    private fun viewModel() = PlayerViewModel(player, quranText)

    @Test
    fun `the last position is restored once on start`() {
        viewModel()
        assertEquals(1, player.restoreCalls)
    }

    @Test
    fun `player bar state mirrors now playing with the surah name`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            player.nowPlaying.value = NowPlaying(2, 255, Track.ARABIC, RecitationMode.ARABIC_ENGLISH, true, false)
            val state = awaitWhere { it.nowPlaying != null }
            assertEquals(255, state.nowPlaying?.ayah)
            assertEquals("Al-Baqara", state.surahName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle, next, previous and stop drive the player`() = runTest {
        val viewModel = viewModel()
        player.nowPlaying.value = NowPlaying(2, 5, Track.ARABIC, RecitationMode.ARABIC_ONLY, true, false)

        viewModel.togglePlayPause()
        assertEquals(false, player.nowPlaying.value?.isPlaying)

        viewModel.nextAyah()
        assertEquals(6, player.nowPlaying.value?.ayah)

        viewModel.previousAyah()
        assertEquals(5, player.nowPlaying.value?.ayah)

        viewModel.stop()
        assertNull(player.nowPlaying.value)
    }

    @Test
    fun `playback errors surface until consumed`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            player.error.value = "Network gone"
            assertEquals("Network gone", awaitWhere { it.error == "Network gone" }.error)

            viewModel.consumeError()
            assertNull(awaitWhere { it.error == null }.error)

            player.error.value = "Network gone again"
            assertEquals("Network gone again", awaitWhere { it.error == "Network gone again" }.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `falls back when surah names are unavailable`() = runTest {
        quranText.failure = IllegalStateException("disk on fire")
        val viewModel = viewModel()
        viewModel.uiState.test {
            player.nowPlaying.value = NowPlaying(2, 255, Track.ARABIC, RecitationMode.ARABIC_ONLY, true, false)
            assertNull(awaitWhere { it.nowPlaying != null }.surahName)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
