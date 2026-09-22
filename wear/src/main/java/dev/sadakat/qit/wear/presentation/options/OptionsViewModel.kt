package dev.sadakat.qit.wear.presentation.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.SleepOption
import dev.sadakat.qit.core.domain.player.SleepTimerStatus
import dev.sadakat.qit.core.domain.repository.QuranSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The repeat choices the watch offers; the phone can also set ranges, which the watch shows as Off. */
enum class RepeatSelection { OFF, THREE_TIMES, FOREVER }

/** The sleep-timer choices the watch offers. */
enum class SleepSelection { OFF, MINUTES_15, MINUTES_30, MINUTES_60, END_OF_SURAH }

data class WearOptionsUiState(
    val speed: PlaybackSpeed = PlaybackSpeed.X1,
    val repeat: RepeatSelection = RepeatSelection.OFF,
    val sleep: SleepSelection = SleepSelection.OFF,
    /** Remaining milliseconds of a running timer; null when it isn't counting. */
    val sleepRemainingMs: Long? = null,
    /** True while the timer runs to the end of the surah. */
    val sleepAtEndOfSurah: Boolean = false,
)

/** Playback options: speed, ayah repeat and the sleep timer, driven straight through the player. */
@HiltViewModel
class OptionsViewModel @Inject constructor(settings: QuranSettings, private val player: QuranPlayer) : ViewModel() {

    // Which minutes option is running: the timer's status no longer says, so the watch remembers.
    private val sleepChoice = MutableStateFlow<SleepSelection?>(null)

    private val speed = combine(player.nowPlaying, settings.playbackSpeed) { nowPlaying, saved ->
        // While something is queued the player leads; otherwise the remembered choice does.
        nowPlaying?.speed ?: saved
    }

    val uiState: StateFlow<WearOptionsUiState> = combine(
        speed,
        player.nowPlaying,
        player.sleepTimer,
        sleepChoice,
    ) { speed, nowPlaying, sleepTimer, choice ->
        WearOptionsUiState(
            speed = speed,
            repeat = selectionOf(nowPlaying?.repeat),
            sleep = sleepSelectionOf(sleepTimer, choice),
            sleepRemainingMs = when (sleepTimer) {
                is SleepTimerStatus.Counting -> sleepTimer.remainingMs
                is SleepTimerStatus.FadingOut -> sleepTimer.remainingMs
                SleepTimerStatus.Off, SleepTimerStatus.EndOfSurah -> null
            },
            sleepAtEndOfSurah = sleepTimer is SleepTimerStatus.EndOfSurah,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearOptionsUiState())

    fun selectSpeed(speed: PlaybackSpeed) = player.setSpeed(speed)

    fun selectRepeat(selection: RepeatSelection) = player.setRepeat(
        when (selection) {
            RepeatSelection.OFF -> RepeatSetting.Off
            RepeatSelection.THREE_TIMES -> RepeatSetting.Ayah(times = 3)
            RepeatSelection.FOREVER -> RepeatSetting.Ayah(times = null)
        },
    )

    fun selectSleep(selection: SleepSelection) {
        sleepChoice.value = selection.takeIf { it != SleepSelection.OFF }
        when (selection) {
            SleepSelection.OFF -> player.setSleepTimer(null)
            SleepSelection.MINUTES_15 -> player.setSleepTimer(SleepOption.Minutes(SLEEP_MINUTES_15))
            SleepSelection.MINUTES_30 -> player.setSleepTimer(SleepOption.Minutes(SLEEP_MINUTES_30))
            SleepSelection.MINUTES_60 -> player.setSleepTimer(SleepOption.Minutes(SLEEP_MINUTES_60))
            SleepSelection.END_OF_SURAH -> player.setSleepTimer(SleepOption.EndOfSurah)
        }
    }

    private fun selectionOf(repeat: RepeatSetting?): RepeatSelection = when (repeat) {
        null, RepeatSetting.Off, is RepeatSetting.Range -> RepeatSelection.OFF
        is RepeatSetting.Ayah -> if (repeat.times == null) RepeatSelection.FOREVER else RepeatSelection.THREE_TIMES
    }

    private companion object {
        const val SLEEP_MINUTES_15 = 15
        const val SLEEP_MINUTES_30 = 30
        const val SLEEP_MINUTES_60 = 60
    }

    private fun sleepSelectionOf(status: SleepTimerStatus, choice: SleepSelection?): SleepSelection = when (status) {
        SleepTimerStatus.Off -> SleepSelection.OFF

        SleepTimerStatus.EndOfSurah -> SleepSelection.END_OF_SURAH

        // Counting or fading: the option the user picked, or Off if the watch didn't see the pick.
        is SleepTimerStatus.Counting, is SleepTimerStatus.FadingOut -> choice ?: SleepSelection.OFF
    }
}
