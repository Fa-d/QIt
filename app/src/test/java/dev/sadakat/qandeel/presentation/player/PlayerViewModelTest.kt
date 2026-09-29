package dev.sadakat.qandeel.presentation.player

import app.cash.turbine.test
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.model.ReadingPrefs
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.model.Track
import dev.sadakat.qandeel.core.domain.model.WordByWord
import dev.sadakat.qandeel.core.domain.player.NowPlaying
import dev.sadakat.qandeel.core.domain.player.PlaybackError
import dev.sadakat.qandeel.core.domain.player.PlaybackProgress
import dev.sadakat.qandeel.core.domain.player.PlaybackSpeed
import dev.sadakat.qandeel.core.domain.player.RepeatSetting
import dev.sadakat.qandeel.core.domain.player.SleepOption
import dev.sadakat.qandeel.core.domain.player.SleepTimerStatus
import dev.sadakat.qandeel.core.domain.player.WordPointer
import dev.sadakat.qandeel.core.testing.FakeAudioTimings
import dev.sadakat.qandeel.core.testing.FakeQuranPlayer
import dev.sadakat.qandeel.core.testing.FakeQuranSettings
import dev.sadakat.qandeel.core.testing.FakeQuranText
import dev.sadakat.qandeel.core.testing.FakeWordMeanings
import dev.sadakat.qandeel.core.testing.MainDispatcherRule
import dev.sadakat.qandeel.core.testing.TestQuran
import dev.sadakat.qandeel.presentation.awaitWhere
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
    private val settings = FakeQuranSettings()

    private val timings = FakeAudioTimings()
    private val wordMeanings = FakeWordMeanings(
        bySurah = mapOf(2 to mapOf(0 to listOf("In (the) name"), 255 to listOf("Allah", "(there is) no"))),
    )

    private fun viewModel() = PlayerViewModel(player, quranText, settings, timings, wordMeanings)

    private fun playing(surah: Int = 2, ayah: Int = 255, mode: RecitationMode = RecitationMode.ARABIC_ENGLISH) =
        NowPlaying(surah, ayah, Track.ARABIC, mode, isPlaying = true, isBuffering = false)

    @Test
    fun `the last position is restored once on start`() {
        viewModel()
        assertEquals(1, player.restoreCalls)
    }

    @Test
    fun `state mirrors now playing with the surah name, the ayah and its translation`() = runTest {
        viewModel().uiState.test {
            player.nowPlaying.value = playing()
            val state = awaitWhere { it.ayahArabic != null }
            assertEquals(255, state.nowPlaying?.ayah)
            assertEquals("Al-Baqara", state.surahName)
            assertEquals(TestQuran.ayahs(2)[254].arabic, state.ayahArabic)
            assertEquals(TestQuran.ayahs(2)[254].english, state.ayahTranslation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no translation in Arabic-only mode or when translations are hidden`() = runTest {
        viewModel().uiState.test {
            player.nowPlaying.value = playing(mode = RecitationMode.ARABIC_ONLY)
            assertNull(awaitWhere { it.ayahArabic != null }.ayahTranslation)

            player.nowPlaying.value = playing(mode = RecitationMode.ARABIC_BANGLA)
            awaitWhere { it.ayahTranslation == TestQuran.ayahs(2)[254].bangla }

            settings.readingPrefs.value = ReadingPrefs(showTranslation = false)
            assertNull(awaitWhere { it.nowPlaying != null && it.ayahTranslation == null }.ayahTranslation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the basmala has no ayah text of its own`() = runTest {
        viewModel().uiState.test {
            player.nowPlaying.value = playing(ayah = 0)
            val state = awaitWhere { it.nowPlaying?.ayah == 0 && it.surahName != null }
            assertNull(state.ayahArabic)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `transport drives the player`() = runTest {
        val viewModel = viewModel()
        player.nowPlaying.value = playing(ayah = 5)

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
    fun `changing the mode remembers it and continues the same ayah in it`() = runTest {
        val viewModel = viewModel()
        player.nowPlaying.value = playing(ayah = 7)

        viewModel.setMode(RecitationMode.ARABIC_BANGLA)

        assertEquals(RecitationMode.ARABIC_BANGLA, settings.mode.value)
        assertEquals(FakeQuranPlayer.PlayCall(2, 7, RecitationMode.ARABIC_BANGLA), player.playCalls.last())
    }

    @Test
    fun `mode changes do nothing when nothing is queued`() = runTest {
        val viewModel = viewModel()
        viewModel.setMode(RecitationMode.ARABIC_ONLY)
        assertEquals(emptyList<FakeQuranPlayer.PlayCall>(), player.playCalls)
    }

    @Test
    fun `changing the voice remembers it and continues the same ayah with it`() = runTest {
        val viewModel = viewModel()
        player.nowPlaying.value = NowPlaying(
            2,
            7,
            Track.ARABIC,
            RecitationMode.ARABIC_BANGLA,
            isPlaying = true,
            isBuffering = false,
            voice = BanglaVoice.DEFAULT,
        )

        viewModel.setVoice(BanglaVoice.SAYED_ISMAT_TOHA)

        assertEquals(BanglaVoice.SAYED_ISMAT_TOHA, settings.banglaVoice.value)
        assertEquals(FakeQuranPlayer.PlayCall(2, 7, RecitationMode.ARABIC_BANGLA), player.playCalls.last())
    }

    @Test
    fun `changing the voice while paused re-queues it paused`() = runTest {
        val viewModel = viewModel()
        player.nowPlaying.value = NowPlaying(
            2,
            7,
            Track.ARABIC,
            RecitationMode.ARABIC_BANGLA,
            isPlaying = false,
            isBuffering = false,
            voice = BanglaVoice.DEFAULT,
        )

        viewModel.setVoice(BanglaVoice.SAYED_ISMAT_TOHA)

        assertEquals(
            FakeQuranPlayer.PlayCall(2, 7, RecitationMode.ARABIC_BANGLA, playWhenReady = false),
            player.playCalls.last(),
        )
    }

    @Test
    fun `voice changes only restart arabic and bangla, and only another voice`() = runTest {
        val viewModel = viewModel()

        // Arabic + English: stored, nothing restarted.
        player.nowPlaying.value = playing(ayah = 7)
        viewModel.setVoice(BanglaVoice.SAYED_ISMAT_TOHA)
        assertEquals(emptyList<FakeQuranPlayer.PlayCall>(), player.playCalls)

        // Arabic + Bangla, already read by the picked voice.
        player.nowPlaying.value = NowPlaying(
            2,
            7,
            Track.ARABIC,
            RecitationMode.ARABIC_BANGLA,
            isPlaying = true,
            isBuffering = false,
            voice = BanglaVoice.SAYED_ISMAT_TOHA,
        )
        viewModel.setVoice(BanglaVoice.SAYED_ISMAT_TOHA)
        assertEquals(emptyList<FakeQuranPlayer.PlayCall>(), player.playCalls)
    }

    @Test
    fun `the ui state carries the playing voice`() = runTest {
        val viewModel = viewModel()
        player.nowPlaying.value = NowPlaying(
            2,
            7,
            Track.ARABIC,
            RecitationMode.ARABIC_BANGLA,
            isPlaying = true,
            isBuffering = false,
            voice = BanglaVoice.SHAREEF_BAEZEED_MAHMOOD,
        )

        viewModel.uiState.test {
            assertEquals(BanglaVoice.SHAREEF_BAEZEED_MAHMOOD, awaitWhere { it.nowPlaying != null }.voice)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repeat, speed and the sleep timer go to the player`() = runTest {
        val viewModel = viewModel()
        player.nowPlaying.value = playing()
        viewModel.uiState.test {
            viewModel.setRepeat(RepeatSetting.Ayah(3))
            viewModel.setSpeed(PlaybackSpeed.X1_25)
            viewModel.setSleepTimer(SleepOption.Minutes(15))

            val state = awaitWhere { it.sleepTimer is SleepTimerStatus.Counting }
            assertEquals(RepeatSetting.Ayah(3), state.nowPlaying?.repeat)
            assertEquals(PlaybackSpeed.X1_25, state.nowPlaying?.speed)
            assertEquals(SleepTimerStatus.Counting(900_000L), state.sleepTimer)

            viewModel.setSleepTimer(null)
            assertEquals(SleepTimerStatus.Off, awaitWhere { it.sleepTimer == SleepTimerStatus.Off }.sleepTimer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a dismissed error stays hidden until the player reports a new failure`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            player.error.value = PlaybackError.NETWORK
            awaitWhere { it.error != null }

            viewModel.consumeError()
            awaitWhere { it.error == null }

            // A retry clears the error; the same failure again is shown again.
            player.error.value = null
            player.error.value = PlaybackError.NETWORK
            assertEquals(PlaybackError.NETWORK, awaitWhere { it.error != null }.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry goes to the player`() {
        player.error.value = PlaybackError.NETWORK

        viewModel().retry()

        assertEquals(1, player.retryCalls)
    }

    @Test
    fun `the playing ayah's word meanings are shown while word by word is on`() = runTest {
        viewModel().uiState.test {
            player.nowPlaying.value = playing()
            assertEquals(emptyList<String>(), awaitWhere { it.ayahArabic != null }.ayahMeanings)

            settings.readingPrefs.value = ReadingPrefs(wordByWord = WordByWord.ENGLISH)
            assertEquals(listOf("Allah", "(there is) no"), awaitWhere { it.ayahMeanings.isNotEmpty() }.ayahMeanings)

            // The basmala has its meanings too, though it has no ayah text of its own.
            player.nowPlaying.value = playing(ayah = 0)
            assertEquals(listOf("In (the) name"), awaitWhere { it.nowPlaying?.ayah == 0 }.ayahMeanings)

            settings.readingPrefs.value = ReadingPrefs(wordByWord = WordByWord.OFF)
            assertEquals(emptyList<String>(), awaitWhere { it.ayahMeanings.isEmpty() }.ayahMeanings)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the pointer mirrors the player's`() = runTest {
        val viewModel = viewModel()
        viewModel.pointer.test {
            assertEquals(WordPointer.Off, awaitItem())
            player.pointer.value = WordPointer.Reciting(3)
            assertEquals(WordPointer.Reciting(3), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `progress mirrors the player`() = runTest {
        val viewModel = viewModel()
        viewModel.progress.test {
            assertEquals(PlaybackProgress.START, awaitItem())
            player.progress.value = PlaybackProgress(1, 2, 3)
            assertEquals(PlaybackProgress(1, 2, 3), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `seeking the surah goes to the player`() {
        viewModel().seekTo(42_000)

        assertEquals(listOf(42_000L), player.seekCalls)
    }

    @Test
    fun `knows where each ayah starts in the surah, to name the ayah at a time`() = runTest {
        timings.defaultDurationMs = 1_000 // every file 1 s: Al-Fatiha in Arabic is 7 s, no basmala
        player.nowPlaying.value = playing(surah = 1, ayah = 1, mode = RecitationMode.ARABIC_ONLY)
        viewModel().uiState.test {
            val state = awaitWhere { it.ayahStartsMs.isNotEmpty() }
            assertEquals(listOf(0L, 1_000L, 2_000L, 3_000L, 4_000L, 5_000L, 6_000L), state.ayahStartsMs)
            assertEquals(4, state.ayahAt(3_500))
            assertEquals(7, state.ayahAt(99_000))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `with a basmala and a translation, an ayah starts at its arabic`() = runTest {
        timings.defaultDurationMs = 1_000 // Al-Ikhlaas + English: 2 basmala files, then ar/en per ayah
        player.nowPlaying.value = playing(surah = 112, ayah = 1, mode = RecitationMode.ARABIC_ENGLISH)
        viewModel().uiState.test {
            val state = awaitWhere { it.ayahStartsMs.isNotEmpty() }
            assertEquals(listOf(2_000L, 4_000L, 6_000L, 8_000L), state.ayahStartsMs)
            assertEquals(0, state.ayahAt(1_500)) // the basmala
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no ayah to name while the lengths are unknown`() {
        assertEquals(null, PlayerUiState().ayahAt(3_500))
    }
}
