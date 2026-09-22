package dev.sadakat.qit.presentation.player

import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.SleepOption

/** Everything the full player can do; the host wires it to [PlayerViewModel] and navigation. */
data class NowPlayingActions(
    val onTogglePlayPause: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    /** Moves to a position (ms) in the whole surah. */
    val onSeek: (Long) -> Unit,
    val onModeChange: (RecitationMode) -> Unit,
    val onRepeatChange: (RepeatSetting) -> Unit,
    val onSpeedChange: (PlaybackSpeed) -> Unit,
    /** Starts a sleep timer, or cancels it with null. */
    val onSleepTimerChange: (SleepOption?) -> Unit,
    val onOpenReader: (surah: Int, ayah: Int) -> Unit,
    val onStop: () -> Unit,
)
