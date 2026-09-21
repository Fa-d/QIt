package dev.sadakat.qit.presentation.surahlist

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
import dev.sadakat.qit.presentation.awaitWhere
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SurahListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val quranText = FakeQuranText()
    private val settings = FakeQuranSettings()
    private val downloads = FakeSurahDownloads()
    private val player = FakeQuranPlayer()

    private fun viewModel() = SurahListViewModel(quranText, settings, downloads, player)

    @Test
    fun `loads all surahs in order`() = runTest {
        viewModel().uiState.test {
            val state = awaitWhere { it.surahs.isNotEmpty() }
            assertEquals(114, state.surahs.size)
            assertEquals(1, state.surahs.first().number)
            assertEquals(114, state.surahs.last().number)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search filters by number, english name, meaning and arabic name`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surahs.size == 114 }

            viewModel.onSearchQueryChange("baqara")
            assertEquals(listOf(2), awaitWhere { it.query == "baqara" }.surahs.map { it.number })

            viewModel.onSearchQueryChange("cow")
            assertEquals(listOf(2), awaitWhere { it.query == "cow" }.surahs.map { it.number })

            viewModel.onSearchQueryChange("mankind")
            assertEquals(listOf(114), awaitWhere { it.query == "mankind" }.surahs.map { it.number })

            viewModel.onSearchQueryChange("الفاتحة")
            assertEquals(listOf(1), awaitWhere { it.query == "الفاتحة" }.surahs.map { it.number })

            // An exact surah number; the remaining 9s here come from the test data's
            // placeholder names ("Surah 19"), real surah names contain no digits.
            viewModel.onSearchQueryChange("9")
            assertTrue(9 in awaitWhere { it.query == "9" }.surahs.map { it.number })

            viewModel.onSearchQueryChange("112")
            assertEquals(listOf(112), awaitWhere { it.query == "112" }.surahs.map { it.number })

            viewModel.onSearchQueryChange("zzz")
            assertTrue(awaitWhere { it.query == "zzz" }.surahs.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `download indicator follows the state of the current mode tracks`() = runTest {
        settings.setMode(RecitationMode.ARABIC_ENGLISH)
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(SurahDownloadState.NotDownloaded, awaitWhere { it.surahs.isNotEmpty() }.downloadStates[2])

            // A partially downloaded track shows as downloading...
            downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloading(1, 7))
            val partial = awaitWhere { it.downloadStates[2] is SurahDownloadState.Downloading }.downloadStates[2]
            assertEquals(SurahDownloadState.Downloading(1, 7), partial)

            // ...but Arabic fully downloaded alone is not enough: the mode also needs the translation.
            downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloaded)
            assertEquals(
                SurahDownloadState.NotDownloaded,
                awaitWhere {
                    it.downloadStates[2] is SurahDownloadState.NotDownloaded && it.surahs.isNotEmpty()
                }.downloadStates[2],
            )

            downloads.setState(2, Track.ENGLISH, SurahDownloadState.Downloaded)
            assertEquals(
                SurahDownloadState.Downloaded,
                awaitWhere { it.downloadStates[2] is SurahDownloadState.Downloaded }.downloadStates[2],
            )

            downloads.setState(2, Track.ENGLISH, SurahDownloadState.Downloading(3, 286))
            assertTrue(
                awaitWhere {
                    it.downloadStates[2] is SurahDownloadState.Downloading
                }.downloadStates[2] is SurahDownloadState.Downloading,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `continue listening card appears for the last position`() = runTest {
        settings.lastPosition.value = LastPosition(AyahRef(2, 255), RecitationMode.ARABIC_ENGLISH)
        viewModel().uiState.test {
            val card = awaitWhere { it.continueListening != null }.continueListening
            assertEquals(ContinueListening(2, "Al-Baqara", 255), card)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no continue listening card before anything played`() = runTest {
        viewModel().uiState.test {
            assertNull(awaitWhere { it.surahs.isNotEmpty() }.continueListening)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `continue listening plays the last position in its saved mode`() = runTest {
        settings.lastPosition.value = LastPosition(AyahRef(2, 255), RecitationMode.ARABIC_ENGLISH)
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.continueListening != null }
            viewModel.continueListening()
            assertEquals(listOf(FakeQuranPlayer.PlayCall(2, 255, RecitationMode.ARABIC_ENGLISH)), player.playCalls)
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
