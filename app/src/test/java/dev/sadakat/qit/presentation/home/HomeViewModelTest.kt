package dev.sadakat.qit.presentation.home

import app.cash.turbine.test
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.testing.FakeQuranPlayer
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.FakeQuranText
import dev.sadakat.qit.core.testing.FakeSurahDownloads
import dev.sadakat.qit.core.testing.MainDispatcherRule
import dev.sadakat.qit.presentation.awaitWhere
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val quranText = FakeQuranText()
    private val settings = FakeQuranSettings()
    private val downloads = FakeSurahDownloads()
    private val player = FakeQuranPlayer()

    private fun viewModel() = HomeViewModel(quranText, settings, downloads, player)

    @Test
    fun `loads every surah and the thirty juz`() = runTest {
        viewModel().uiState.test {
            val state = awaitWhere { !it.isLoading }
            assertEquals(114, state.surahs.size)
            assertEquals(30, state.juz.size)
            assertEquals(JuzRowUi(2, AyahRef(2, 142), "Al-Baqara"), state.juz[1])
            assertFalse(state.loadFailed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search filters by number, english name, meaning and arabic name`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surahs.size == 114 }

            viewModel.onQueryChange("baqara")
            assertEquals(listOf(2), awaitWhere { it.query == "baqara" }.surahs.map { it.surah.number })

            viewModel.onQueryChange("cow")
            assertEquals(listOf(2), awaitWhere { it.query == "cow" }.surahs.map { it.surah.number })

            viewModel.onQueryChange("الفاتحة")
            assertEquals(listOf(1), awaitWhere { it.query == "الفاتحة" }.surahs.map { it.surah.number })

            viewModel.onQueryChange("112")
            assertEquals(listOf(112), awaitWhere { it.query == "112" }.surahs.map { it.surah.number })

            viewModel.onQueryChange("zzz")
            assertTrue(awaitWhere { it.query == "zzz" }.surahs.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a verse reference offers a jump to that ayah`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { !it.isLoading }

            viewModel.onQueryChange("2:255")
            val state = awaitWhere { it.query == "2:255" }
            assertEquals(AyahJumpUi(AyahRef(2, 255), "Al-Baqara"), state.jumpTarget)
            assertTrue(state.isSearching)

            viewModel.onQueryChange("2:999")
            assertNull(awaitWhere { it.query == "2:999" }.jumpTarget)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `browse mode switches between surahs and juz`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(BrowseMode.SURAH, awaitWhere { !it.isLoading }.browse)
            viewModel.onBrowseChange(BrowseMode.JUZ)
            assertEquals(BrowseMode.JUZ, awaitWhere { it.browse == BrowseMode.JUZ }.browse)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `download state follows the current mode's tracks and the playing surah is marked`() = runTest {
        settings.setMode(RecitationMode.ARABIC_ENGLISH)
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { !it.isLoading }

            downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloaded)
            downloads.setState(2, Track.ENGLISH, SurahDownloadState.Downloaded)
            player.play(2, 1, RecitationMode.ARABIC_ENGLISH)

            val row = awaitWhere { state -> state.surahs.getOrNull(1)?.isPlaying == true }.surahs[1]
            assertEquals(SurahDownloadState.Downloaded, row.download)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the card shows the saved position until something is queued, then what's queued`() = runTest {
        settings.lastPosition.value = LastPosition(AyahRef(2, 255), RecitationMode.ARABIC_ENGLISH)
        val viewModel = viewModel()
        viewModel.uiState.test {
            val saved = awaitWhere { it.continueListening != null }.continueListening!!
            assertEquals(
                ContinueListeningUi(2, "Al-Baqara", "البقرة", 255, 286, isCurrent = false, isPlaying = false),
                saved,
            )

            player.nowPlaying.value = NowPlaying(18, 10, Track.ARABIC, RecitationMode.ARABIC_ONLY, true, false)
            val current = awaitWhere { it.continueListening?.isCurrent == true }.continueListening!!
            assertEquals(18, current.surah)
            assertEquals(10, current.ayah)
            assertTrue(current.isPlaying)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no card before anything has played`() = runTest {
        viewModel().uiState.test {
            assertNull(awaitWhere { !it.isLoading }.continueListening)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the card's button resumes the saved position in its mode, or toggles what's queued`() = runTest {
        settings.lastPosition.value = LastPosition(AyahRef(2, 255), RecitationMode.ARABIC_ENGLISH)
        val viewModel = viewModel()

        viewModel.onContinuePlayPause()
        assertEquals(listOf(FakeQuranPlayer.PlayCall(2, 255, RecitationMode.ARABIC_ENGLISH)), player.playCalls)

        viewModel.onContinuePlayPause()
        assertEquals(false, player.nowPlaying.value?.isPlaying)
        assertEquals(1, player.playCalls.size)
    }

    @Test
    fun `a failing text source shows the error state`() = runTest {
        quranText.failure = IllegalStateException("disk on fire")
        viewModel().uiState.test {
            assertTrue(awaitWhere { it.loadFailed }.loadFailed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry goes back to loading and then loads the surahs`() = runTest {
        quranText.failure = IllegalStateException("disk on fire")
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertTrue(awaitWhere { it.loadFailed }.loadFailed)

            quranText.failure = null
            viewModel.retry()

            assertTrue(awaitWhere { it.isLoading }.isLoading) // back to loading, not still failed
            val state = awaitWhere { !it.isLoading && !it.loadFailed }
            assertEquals(114, state.surahs.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
