package dev.sadakat.qandeel.wear.presentation.options

import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.player.PlaybackSpeed
import dev.sadakat.qandeel.core.domain.player.RepeatSetting
import dev.sadakat.qandeel.core.domain.player.SleepOption
import dev.sadakat.qandeel.core.domain.player.SleepTimerStatus
import dev.sadakat.qandeel.core.testing.FakeQuranPlayer
import dev.sadakat.qandeel.core.testing.FakeQuranSettings
import dev.sadakat.qandeel.core.testing.MainDispatcherRule
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
class OptionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeQuranSettings()
    private val player = FakeQuranPlayer()

    private fun viewModel() = OptionsViewModel(settings, player)

    /** A [OptionsViewModel] whose state is being collected eagerly, so `.value` is current. */
    private fun TestScope.collected(): OptionsViewModel {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(mutableListOf()) }
        runCurrent()
        return viewModel
    }

    @Test
    fun `while something is queued the player's speed and repeat lead`() = runTest {
        player.play(2, 1, RecitationMode.ARABIC_BANGLA)
        player.setSpeed(PlaybackSpeed.X1_5)
        player.setRepeat(RepeatSetting.Ayah(times = null))

        val state = collected().uiState.value

        assertEquals(PlaybackSpeed.X1_5, state.speed)
        assertEquals(RepeatSelection.FOREVER, state.repeat)
    }

    @Test
    fun `with nothing queued the remembered speed shows`() = runTest {
        settings.setPlaybackSpeed(PlaybackSpeed.X0_75)

        assertEquals(PlaybackSpeed.X0_75, collected().uiState.value.speed)
    }

    @Test
    fun `repeat selections map both ways`() = runTest {
        player.play(2, 1, RecitationMode.ARABIC_BANGLA)
        player.setRepeat(RepeatSetting.Ayah(times = 3))
        val viewModel = collected()

        assertEquals(RepeatSelection.THREE_TIMES, viewModel.uiState.value.repeat)

        viewModel.selectRepeat(RepeatSelection.OFF)
        assertEquals(RepeatSetting.Off, player.nowPlaying.value?.repeat)
        assertEquals(RepeatSelection.OFF, viewModel.uiState.value.repeat)

        viewModel.selectRepeat(RepeatSelection.FOREVER)
        assertEquals(RepeatSelection.FOREVER, viewModel.uiState.value.repeat)
    }

    @Test
    fun `a range set on the phone reads as Off on the watch`() = runTest {
        player.play(2, 1, RecitationMode.ARABIC_BANGLA)
        player.setRepeat(RepeatSetting.Range(1, 5, times = null))

        assertEquals(RepeatSelection.OFF, collected().uiState.value.repeat)
    }

    @Test
    fun `selecting a speed drives the player`() = runTest {
        // Something queued: the player leads the shown speed, so its change shows up directly.
        player.play(1, 1, RecitationMode.ARABIC_ONLY)
        val viewModel = collected()

        viewModel.selectSpeed(PlaybackSpeed.X1_25)
        assertEquals(PlaybackSpeed.X1_25, viewModel.uiState.value.speed)
        assertEquals(PlaybackSpeed.X1_25, player.speed)
    }

    @Test
    fun `starting a sleep timer reports the countdown and the picked option`() = runTest {
        val viewModel = collected()

        viewModel.selectSleep(SleepSelection.MINUTES_30)
        val state = viewModel.uiState.value
        assertEquals(SleepSelection.MINUTES_30, state.sleep)
        assertEquals(30 * 60_000L, state.sleepRemainingMs)
        assertEquals(SleepOption.Minutes(30), player.sleepOption)

        // The timer firing resets the selection as if it had never been picked.
        player.sleepTimer.value = SleepTimerStatus.Off
        assertEquals(SleepSelection.OFF, viewModel.uiState.value.sleep)
    }

    @Test
    fun `a timer started on the phone shows as Off until the watch picks one`() = runTest {
        player.setSleepTimer(SleepOption.Minutes(15))

        // The status no longer says which option started it, so nothing is marked selected.
        val state = collected().uiState.value
        assertEquals(SleepSelection.OFF, state.sleep)
        assertEquals(15 * 60_000L, state.sleepRemainingMs)
    }

    @Test
    fun `end of surah marks its option and the header note`() = runTest {
        val viewModel = collected()

        viewModel.selectSleep(SleepSelection.END_OF_SURAH)
        val state = viewModel.uiState.value
        assertEquals(SleepSelection.END_OF_SURAH, state.sleep)
        assertEquals(true, state.sleepAtEndOfSurah)
        assertNull(state.sleepRemainingMs)
    }

    @Test
    fun `selecting Off cancels a running timer`() = runTest {
        val viewModel = collected()

        viewModel.selectSleep(SleepSelection.MINUTES_15)
        viewModel.selectSleep(SleepSelection.OFF)

        assertEquals(SleepSelection.OFF, viewModel.uiState.value.sleep)
        assertNull(player.sleepOption)
    }

    @Test
    fun `a fading timer keeps the picked option selected`() = runTest {
        val viewModel = collected()

        viewModel.selectSleep(SleepSelection.MINUTES_15)
        player.sleepTimer.value = SleepTimerStatus.FadingOut(3_000L)

        val state = viewModel.uiState.value
        assertEquals(SleepSelection.MINUTES_15, state.sleep)
        assertEquals(3_000L, state.sleepRemainingMs)
        assertEquals(false, state.sleepAtEndOfSurah)
    }
}
