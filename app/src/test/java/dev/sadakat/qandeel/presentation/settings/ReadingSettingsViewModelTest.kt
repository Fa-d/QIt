package dev.sadakat.qandeel.presentation.settings

import app.cash.turbine.test
import dev.sadakat.qandeel.core.domain.model.ArabicTextSize
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.model.ReadingPrefs
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.model.ThemeMode
import dev.sadakat.qandeel.core.domain.model.Track
import dev.sadakat.qandeel.core.domain.model.WordByWord
import dev.sadakat.qandeel.core.domain.player.NowPlaying
import dev.sadakat.qandeel.core.testing.FakeQuranPlayer
import dev.sadakat.qandeel.core.testing.FakeQuranSettings
import dev.sadakat.qandeel.core.testing.MainDispatcherRule
import dev.sadakat.qandeel.presentation.awaitWhere
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReadingSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeQuranSettings()
    private val player = FakeQuranPlayer()

    private fun viewModel() = ReadingSettingsViewModel(settings, player)

    @Test
    fun `the state mirrors the stored reading prefs`() = runTest {
        settings.readingPrefs.value = ReadingPrefs(themeMode = ThemeMode.DARK, dynamicColor = true)
        viewModel().uiState.test {
            val state = awaitWhere { it.prefs.themeMode == ThemeMode.DARK }
            assertEquals(ThemeMode.DARK, state.prefs.themeMode)
            assertEquals(true, state.prefs.dynamicColor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the state mirrors the stored bangla voice`() = runTest {
        settings.banglaVoice.value = BanglaVoice.SAYED_ISMAT_TOHA
        viewModel().uiState.test {
            assertEquals(BanglaVoice.SAYED_ISMAT_TOHA, awaitWhere { it.voice == BanglaVoice.SAYED_ISMAT_TOHA }.voice)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `picking a voice stores it, and restarts arabic and bangla at the current ayah`() = runTest {
        player.nowPlaying.value = NowPlaying(2, 10, Track.ARABIC, RecitationMode.ARABIC_BANGLA, true, false)
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.prefs == ReadingPrefs() }

            viewModel.setBanglaVoice(BanglaVoice.SAYED_ISMAT_TOHA)

            assertEquals(BanglaVoice.SAYED_ISMAT_TOHA, settings.banglaVoice.value)
            assertEquals(listOf(FakeQuranPlayer.PlayCall(2, 10, RecitationMode.ARABIC_BANGLA)), player.playCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `picking a voice while paused doesn't start playback`() = runTest {
        player.nowPlaying.value = NowPlaying(2, 10, Track.ARABIC, RecitationMode.ARABIC_BANGLA, false, false)
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.prefs == ReadingPrefs() }

            viewModel.setBanglaVoice(BanglaVoice.SHAREEF_BAEZEED_MAHMOOD)

            assertEquals(
                listOf(FakeQuranPlayer.PlayCall(2, 10, RecitationMode.ARABIC_BANGLA, playWhenReady = false)),
                player.playCalls,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `picking the playing voice, or another mode's playback, does not restart`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.prefs == ReadingPrefs() }

            // Arabic + Bangla, already read by the picked voice.
            player.nowPlaying.value = NowPlaying(
                2,
                10,
                Track.ARABIC,
                RecitationMode.ARABIC_BANGLA,
                true,
                false,
                voice = BanglaVoice.DEFAULT,
            )
            viewModel.setBanglaVoice(BanglaVoice.DEFAULT)

            // Arabic only: the voice is stored but nothing restarts.
            player.nowPlaying.value = NowPlaying(2, 10, Track.ARABIC, RecitationMode.ARABIC_ONLY, true, false)
            viewModel.setBanglaVoice(BanglaVoice.SHAREEF_BAEZEED_MAHMOOD)

            assertEquals(BanglaVoice.SHAREEF_BAEZEED_MAHMOOD, settings.banglaVoice.value)
            assertTrue(player.playCalls.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `every action updates its own pref`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { it.prefs == ReadingPrefs() }

            viewModel.setArabicTextSize(ArabicTextSize.XXLARGE)
            assertEquals(
                ArabicTextSize.XXLARGE,
                awaitWhere {
                    it.prefs.arabicTextSize == ArabicTextSize.XXLARGE
                }.prefs.arabicTextSize,
            )

            viewModel.setShowTranslation(false)
            assertEquals(false, awaitWhere { !it.prefs.showTranslation }.prefs.showTranslation)

            viewModel.setFollowAlong(false)
            assertEquals(false, awaitWhere { !it.prefs.followAlong }.prefs.followAlong)

            viewModel.setWordByWord(WordByWord.BANGLA)
            assertEquals(
                WordByWord.BANGLA,
                awaitWhere { it.prefs.wordByWord == WordByWord.BANGLA }.prefs.wordByWord,
            )

            viewModel.setThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitWhere { it.prefs.themeMode == ThemeMode.LIGHT }.prefs.themeMode)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
