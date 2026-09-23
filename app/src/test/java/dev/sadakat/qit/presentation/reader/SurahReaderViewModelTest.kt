package dev.sadakat.qit.presentation.reader

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.sadakat.qit.core.domain.model.ArabicWords
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.BanglaVoice
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.model.WordByWord
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.WordPointer
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.testing.FakeListeningHistory
import dev.sadakat.qit.core.testing.FakeQuranPlayer
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.FakeQuranText
import dev.sadakat.qit.core.testing.FakeSurahDownloads
import dev.sadakat.qit.core.testing.FakeWordMeanings
import dev.sadakat.qit.core.testing.MainDispatcherRule
import dev.sadakat.qit.core.testing.TestQuran
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
    private val history = FakeListeningHistory()
    private val wordMeanings = FakeWordMeanings(bySurah = mapOf(2 to mapOf(1 to listOf("alif lam mim"))))

    private fun viewModel(surah: Int = 2, ayah: Int = 0) = SurahReaderViewModel(
        SavedStateHandle(mapOf("surah" to surah, "ayah" to ayah)),
        quranText,
        settings,
        downloads,
        player,
        watch,
        history,
        wordMeanings,
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
    fun `reading prefs reach the reader state`() = runTest {
        settings.readingPrefs.value = ReadingPrefs(showTranslation = false, followAlong = false)
        viewModel().uiState.test {
            val state = awaitWhere { it.surah != null }
            assertFalse(state.showTranslation)
            assertFalse(state.followAlong)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `translations are shown and the list follows along by default`() = runTest {
        viewModel().uiState.test {
            val state = awaitWhere { it.surah != null }
            assertTrue(state.showTranslation)
            assertTrue(state.followAlong)
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
            assertEquals(
                listOf(2 to RecitationMode.ARABIC_BANGLA.tracks(BanglaVoice.DEFAULT)),
                downloads.downloadRequests,
            )
            assertEquals(
                listOf(2 to RecitationMode.ARABIC_BANGLA.tracks(BanglaVoice.DEFAULT)),
                downloads.removeRequests,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `download and remove use the stored voice's tracks`() = runTest {
        settings.banglaVoice.value = BanglaVoice.SAYED_ISMAT_TOHA
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }

            viewModel.download()
            viewModel.remove()

            val tracks = RecitationMode.ARABIC_BANGLA.tracks(BanglaVoice.SAYED_ISMAT_TOHA)
            assertEquals(listOf(2 to tracks), downloads.downloadRequests)
            assertEquals(listOf(2 to tracks), downloads.removeRequests)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `download state reflects the current mode tracks`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(SurahDownloadState.NotDownloaded, awaitWhere { it.surah != null }.downloadState)
            downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloading(1, 7))
            assertTrue(
                awaitWhere {
                    it.downloadState is SurahDownloadState.Downloading
                }.downloadState is SurahDownloadState.Downloading,
            )
            downloads.setState(2, Track.ARABIC, SurahDownloadState.Downloaded)
            downloads.setState(2, Track.BANGLA, SurahDownloadState.Downloaded)
            assertEquals(
                SurahDownloadState.Downloaded,
                awaitWhere {
                    it.downloadState is SurahDownloadState.Downloaded
                }.downloadState,
            )
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
            assertEquals(listOf(2 to RecitationMode.ARABIC_BANGLA.tracks(BanglaVoice.DEFAULT)), watch.sentSurahs)
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

    @Test
    fun `retry reloads the surah after a failure`() = runTest {
        quranText.failure = IllegalStateException("disk on fire")
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertTrue(awaitWhere { it.loadFailed }.loadFailed)

            quranText.failure = null
            viewModel.retry()

            val state = awaitWhere { it.surah != null && !it.loadFailed }
            assertEquals("Al-Baqara", state.surah?.nameEnglish)
            assertEquals(286, state.ayahs.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `playing from a word and repeating an ayah use the current mode`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }

            viewModel.playFromWord(ayah = 7, word = 2)
            assertEquals(
                listOf(FakeQuranPlayer.PlayFromWordCall(2, 7, 2, RecitationMode.ARABIC_BANGLA)),
                player.playFromWordCalls,
            )

            viewModel.repeatAyah(ayah = 7)
            assertEquals(
                listOf(FakeQuranPlayer.RepeatAyahCall(2, 7, RecitationMode.ARABIC_BANGLA, times = null)),
                player.repeatAyahCalls,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the ayah actions show the ayah, its translation and each word with its meaning`() = runTest {
        wordMeanings.byLanguage = mapOf(
            WordByWord.ENGLISH to mapOf(2 to mapOf(1 to listOf("an ayah", "two one"))),
        )
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }

            viewModel.showAyahActions(1)

            val actions = awaitWhere { it.ayahActions?.words?.all { w -> w.meaning != null } == true }.ayahActions!!
            assertEquals(1, actions.ayah)
            assertEquals(TestQuran.ayahs(2)[0].arabic, actions.arabic)
            assertEquals(TestQuran.ayahs(2)[0].bangla, actions.translation) // the mode's translation
            // English while word by word is off
            assertEquals(listOf("an ayah", "two one"), actions.words.map { it.meaning })
            assertEquals(2 to WordByWord.ENGLISH, wordMeanings.requested.last())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the glossary follows the word-by-word language once one is picked`() = runTest {
        wordMeanings.byLanguage = mapOf(
            WordByWord.BANGLA to mapOf(2 to mapOf(1 to listOf("একটি আয়াত", "দুই এক"))),
        )
        settings.readingPrefs.value = ReadingPrefs(wordByWord = WordByWord.BANGLA)
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.wordMeanings.isNotEmpty() }

            viewModel.showAyahActions(1)

            val actions = awaitWhere { it.ayahActions?.words?.any { w -> w.meaning != null } == true }.ayahActions!!
            assertEquals(listOf("একটি আয়াত", "দুই এক"), actions.words.map { it.meaning })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no meanings are requested while the actions are closed`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }
            val before = wordMeanings.requested.size // the reader's own word-by-word flow may have asked

            viewModel.showAyahActions(1)
            awaitWhere { it.ayahActions?.words?.first()?.meaning != null }
            assertEquals(before + 1, wordMeanings.requested.size) // the glossary's own, once

            viewModel.dismissAyahActions()
            assertNull(awaitWhere { it.ayahActions == null }.ayahActions)
            assertEquals(before + 1, wordMeanings.requested.size) // closed again: nothing more requested
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the words are listed without meanings when none are known`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.surah != null }

            viewModel.showAyahActions(3) // no meanings for ayah 3

            val actions = awaitWhere { it.ayahActions?.ayah == 3 }.ayahActions!!
            assertEquals(TestQuran.ayahs(2)[2].arabic, actions.arabic)
            assertEquals(
                ArabicWords.ranges(actions.arabic).map {
                    actions.arabic.substring(it)
                },
                actions.words.map { it.arabic },
            )
            assertTrue(actions.words.all { it.meaning == null })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `shows how often each ayah was heard, and the surah's progress`() = runTest {
        history.recordHeard(AyahRef(2, 1), atMs = 10)
        history.recordHeard(AyahRef(2, 1), atMs = 20)
        history.recordHeard(AyahRef(2, 3), atMs = 30)

        viewModel().uiState.test {
            val state = awaitWhere { it.surah != null && it.heard.isNotEmpty() }
            assertEquals(listOf(2, 0, 1), state.heard.take(3))
            assertEquals(286, state.heard.size)
            assertEquals(2, state.listening?.ayahsHeard)
            assertEquals(3, state.listening?.totalListens)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no listening summary before anything is heard`() = runTest {
        viewModel().uiState.test {
            val state = awaitWhere { it.surah != null }
            assertNull(state.listening)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the pointer follows this surah's recitation only`() = runTest {
        val viewModel = viewModel()
        viewModel.pointer.test {
            assertEquals(WordPointer.Off, awaitItem())
            player.pointer.value = WordPointer.Reciting(1)
            player.nowPlaying.value =
                NowPlaying(2, 5, Track.ARABIC, RecitationMode.ARABIC_ONLY, isPlaying = true, isBuffering = false)
            assertEquals(WordPointer.Reciting(1), expectMostRecentItem())
            player.nowPlaying.value =
                NowPlaying(3, 5, Track.ARABIC, RecitationMode.ARABIC_ONLY, isPlaying = true, isBuffering = false)
            assertEquals(WordPointer.Off, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `word meanings are loaded in the picked language only while word by word is on`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(emptyMap<Int, List<String>>(), awaitWhere { it.surah != null }.wordMeanings)

            settings.readingPrefs.value = ReadingPrefs(wordByWord = WordByWord.BANGLA)
            assertEquals(listOf("alif lam mim"), awaitWhere { it.wordMeanings.isNotEmpty() }.wordMeanings[1])
            assertEquals(2 to WordByWord.BANGLA, wordMeanings.requested.last())

            settings.readingPrefs.value = ReadingPrefs(wordByWord = WordByWord.OFF)
            assertTrue(awaitWhere { it.wordMeanings.isEmpty() }.wordMeanings.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
