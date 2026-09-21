package dev.sadakat.qit.presentation.reader

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.testing.FakeQuranPlayer
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.FakeQuranText
import dev.sadakat.qit.core.testing.FakeSurahDownloads
import dev.sadakat.qit.core.testing.MainDispatcherRule
import dev.sadakat.qit.presentation.awaitWhere
import dev.sadakat.qit.watch.FakeWatchConnection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SurahReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val quranText = FakeQuranText()
    private val settings = FakeQuranSettings()
    private val downloads = FakeSurahDownloads()
    private val player = FakeQuranPlayer()
    private val watch = FakeWatchConnection()

    private fun viewModel(surah: Int = 2, ayah: Int = 0) = SurahReaderViewModel(
        SavedStateHandle(mapOf("surah" to surah, "ayah" to ayah)),
        quranText,
        settings,
        downloads,
        player,
        watch,
    )

    @Test
    fun `loads the surah and its ayahs`() = runTest {
        viewModel().uiState.test {
            val state = awaitWhere { it.surah != null }
            assertEquals("Al-Baqara", state.surah?.nameEnglish)
            assertEquals(286, state.ayahs.size)
            assertFalse(state.loadFailed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeps the ayah to scroll to from the navigation argument`() = runTest {
        viewModel(ayah = 255).uiState.test {
            assertEquals(255, awaitWhere { it.surah != null }.initialAyah)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `highlights the playing ayah only while this surah plays`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertNull(awaitWhere { it.surah != null }.playingAyah)

            player.nowPlaying.value = NowPlaying(2, 5, Track.ARABIC, RecitationMode.ARABIC_BANGLA, true, false)
            assertEquals(5, awaitWhere { it.playingAyah != null }.playingAyah)

            player.nowPlaying.value = NowPlaying(3, 1, Track.ARABIC, RecitationMode.ARABIC_BANGLA, true, false)
            assertNull(awaitWhere { it.playingAyah == null && it.surah?.number == 2 }.playingAyah)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tapping an ayah plays it in the current mode`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }
            viewModel.playAyah(7)
            assertEquals(listOf(FakeQuranPlayer.PlayCall(2, 7, RecitationMode.ARABIC_BANGLA)), player.playCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `playing the surah starts from the basmala when there is one`() = runTest {
        val viewModel = viewModel(surah = 2)
        viewModel.uiState.test {
            awaitWhere { it.surah != null }
            viewModel.playSurah()
            assertEquals(listOf(FakeQuranPlayer.PlayCall(2, 0, RecitationMode.ARABIC_BANGLA)), player.playCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `playing the surah starts from verse 1 when there is no basmala prefix`() = runTest {
        val viewModel = viewModel(surah = 9)
        viewModel.uiState.test {
            awaitWhere { it.surah != null }
            viewModel.playSurah()
            assertEquals(listOf(FakeQuranPlayer.PlayCall(9, 1, RecitationMode.ARABIC_BANGLA)), player.playCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changing mode persists it and restarts this surah at the current ayah`() = runTest {
        player.nowPlaying.value = NowPlaying(2, 10, Track.ARABIC, RecitationMode.ARABIC_ONLY, true, false)
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }
            viewModel.setMode(RecitationMode.ARABIC_ENGLISH)
            awaitWhere { it.mode == RecitationMode.ARABIC_ENGLISH }
            assertEquals(RecitationMode.ARABIC_ENGLISH, settings.mode.value)
            assertEquals(listOf(FakeQuranPlayer.PlayCall(2, 10, RecitationMode.ARABIC_ENGLISH)), player.playCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changing mode does not restart playback of another surah`() = runTest {
        player.nowPlaying.value = NowPlaying(3, 10, Track.ARABIC, RecitationMode.ARABIC_ONLY, true, false)
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }
            viewModel.setMode(RecitationMode.ARABIC_ENGLISH)
            awaitWhere { it.mode == RecitationMode.ARABIC_ENGLISH }
            assertTrue(player.playCalls.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `download and remove use the current mode tracks`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }
            viewModel.download()
            viewModel.remove()
            assertEquals(listOf(2 to RecitationMode.ARABIC_BANGLA.tracks), downloads.downloadRequests)
            assertEquals(listOf(2 to RecitationMode.ARABIC_BANGLA.tracks), downloads.removeRequests)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `download state reflects the current mode tracks`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(SurahDownloadState.NotDownloaded, awaitWhere { it.surah != null }.downloadState)
            downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloading(1, 7))
            assertTrue(awaitWhere { it.downloadState is SurahDownloadState.Downloading }.downloadState is SurahDownloadState.Downloading)
            downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloaded)
            downloads.setState(2, Track.BANGLA, SurahDownloadState.Downloaded)
            assertEquals(SurahDownloadState.Downloaded, awaitWhere { it.downloadState is SurahDownloadState.Downloaded }.downloadState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `send to watch reports how many watches took the request`() = runTest {
        watch.sendResult = Result.success(2)
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }
            viewModel.sendToWatch()
            assertEquals(ReaderMessage.SentToWatch(2), awaitWhere { it.message != null }.message)
            assertEquals(listOf(2 to RecitationMode.ARABIC_BANGLA.tracks), watch.sentSurahs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `send to watch fails without a reachable watch`() = runTest {
        watch.reachable = false
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }
            viewModel.sendToWatch()
            assertEquals(ReaderMessage.NoWatch, awaitWhere { it.message != null }.message)
            assertTrue(watch.sentSurahs.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `consuming the message clears it`() = runTest {
        watch.reachable = false
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }
            viewModel.sendToWatch()
            awaitWhere { it.message != null }
            viewModel.consumeMessage()
            assertNull(awaitWhere { it.message == null }.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failing text source shows the error state`() = runTest {
        quranText.failure = IllegalStateException("disk on fire")
        viewModel().uiState.test {
            assertTrue(awaitWhere { it.loadFailed }.loadFailed)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
