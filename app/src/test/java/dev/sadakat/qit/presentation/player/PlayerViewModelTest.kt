package dev.sadakat.qit.presentation.player

import app.cash.turbine.test
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.SleepOption
import dev.sadakat.qit.core.domain.player.SleepTimerStatus
import dev.sadakat.qit.core.testing.FakeQuranPlayer
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.FakeQuranText
import dev.sadakat.qit.core.testing.MainDispatcherRule
import dev.sadakat.qit.core.testing.TestQuran
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
    private val settings = FakeQuranSettings()

    private fun viewModel() = PlayerViewModel(player, quranText, settings)

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
    fun `seeking plays the same surah from that ayah in the same mode`() = runTest {
        val viewModel = viewModel()
        player.nowPlaying.value = playing(ayah = 5)

        viewModel.seekToAyah(40)

        assertEquals(FakeQuranPlayer.PlayCall(2, 40, RecitationMode.ARABIC_ENGLISH), player.playCalls.last())
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
    fun `seeking and mode changes do nothing when nothing is queued`() = runTest {
        val viewModel = viewModel()
        viewModel.seekToAyah(3)
        viewModel.setMode(RecitationMode.ARABIC_ONLY)
        assertEquals(emptyList<FakeQuranPlayer.PlayCall>(), player.playCalls)
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
            player.error.value = "Can't reach the audio."
            awaitWhere { it.error != null }

            viewModel.consumeError()
            awaitWhere { it.error == null }

            // A retry clears the error; the same failure again is shown again.
            player.error.value = null
            player.error.value = "Can't reach the audio."
            assertEquals("Can't reach the audio.", awaitWhere { it.error != null }.error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
