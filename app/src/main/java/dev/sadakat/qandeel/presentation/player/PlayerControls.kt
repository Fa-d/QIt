package dev.sadakat.qandeel.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.sadakat.qandeel.R
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.core.designsystem.component.PlayerTokens
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.player.NowPlaying
import dev.sadakat.qandeel.core.domain.player.PlaybackSpeed
import dev.sadakat.qandeel.core.domain.player.RepeatSetting
import dev.sadakat.qandeel.core.domain.player.SleepTimerStatus
import dev.sadakat.qandeel.core.ui.kit.QandeelMenu
import kotlin.math.max

/**
 * The transport, flanked by the two things changed while listening: repeat (for memorizing) on
 * the left, speed on the right. Previous and next move by ayah.
 */
@Composable
fun TransportRow(nowPlaying: NowPlaying, actions: NowPlayingActions, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RepeatButton(nowPlaying, actions.onRepeatChange)
        IconButton(onClick = actions.onPrevious, modifier = Modifier.size(PlayerTokens.CardPlayButtonSize)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.cd_previous_ayah),
                modifier = Modifier.size(QandeelTheme.sizes.iconLarge),
            )
        }
        PlayPauseButton(nowPlaying, actions.onTogglePlayPause)
        IconButton(onClick = actions.onNext, modifier = Modifier.size(PlayerTokens.CardPlayButtonSize)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.cd_next_ayah),
                modifier = Modifier.size(QandeelTheme.sizes.iconLarge),
            )
        }
        SpeedButton(nowPlaying.speed, actions.onSpeedChange)
    }
}

@Composable
private fun PlayPauseButton(nowPlaying: NowPlaying, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        FilledIconButton(onClick = onClick, modifier = Modifier.size(PlayerTokens.PlayButtonSize)) {
            Icon(
                imageVector = if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (nowPlaying.isPlaying) R.string.cd_pause else R.string.cd_play),
                modifier = Modifier.size(PlayerTokens.PlayButtonIconSize),
            )
        }
        if (nowPlaying.isBuffering) {
            CircularProgressIndicator(
                strokeWidth = QandeelTheme.sizes.strokeThin,
                modifier = Modifier
                    .size(PlayerTokens.PlayButtonSize)
                    .testTag("player_buffering"),
            )
        }
    }
}

/** Repeat: lit while something repeats, with how many times ("3", "∞") on a badge. */
@Composable
private fun RepeatButton(nowPlaying: NowPlaying, onRepeatChange: (RepeatSetting) -> Unit) {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    val repeat = nowPlaying.repeat
    val description = if (repeat == RepeatSetting.Off) {
        stringResource(R.string.player_repeat_off)
    } else {
        stringResource(R.string.player_cd_repeat, repeatLabel(repeat))
    }
    IconButton(
        onClick = { dialogOpen = true },
        modifier = Modifier
            .testTag("player_repeat")
            .semantics { contentDescription = description },
    ) {
        BadgedBox(
            badge = {
                repeatBadge(repeat)?.let {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) { Text(it) }
                }
            },
        ) {
            Icon(
                imageVector = Icons.Rounded.Repeat,
                contentDescription = null,
                tint = if (repeat == RepeatSetting.Off) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
    if (dialogOpen) {
        RepeatDialog(
            current = repeat,
            ayah = max(nowPlaying.ayah, 1),
            ayahCount = nowPlaying.ayahCount,
            onConfirm = {
                dialogOpen = false
                onRepeatChange(it)
            },
            onDismiss = { dialogOpen = false },
        )
    }
}

/** The badge on the repeat button: how many times, or ∞; none while off. The range is in the dialog. */
@Composable
private fun repeatBadge(repeat: RepeatSetting): String? {
    val times = when (repeat) {
        RepeatSetting.Off -> return null
        is RepeatSetting.Ayah -> repeat.times
        is RepeatSetting.Range -> repeat.times
    }
    return times?.toString() ?: stringResource(R.string.player_repeat_times_forever)
}

/** The speed as a word-sized button ("1×"); its menu offers the speeds. */
@Composable
private fun SpeedButton(speed: PlaybackSpeed, onSpeedChange: (PlaybackSpeed) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val description = stringResource(R.string.player_cd_speed, speedFactor(speed))
    Box {
        TextButton(
            onClick = { open = true },
            modifier = Modifier
                .testTag("player_speed")
                .semantics { contentDescription = description },
        ) {
            Text(
                text = stringResource(R.string.player_speed, speedFactor(speed)),
                style = MaterialTheme.typography.titleMedium,
                color = if (speed == PlaybackSpeed.X1) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
        QandeelMenu(expanded = open, onDismissRequest = { open = false }) {
            PlaybackSpeed.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.player_speed, speedFactor(option))) },
                    trailingIcon = { if (option == speed) Icon(Icons.Rounded.Check, contentDescription = null) },
                    onClick = {
                        open = false
                        onSpeedChange(option)
                    },
                )
            }
        }
    }
}

/**
 * What plays after the Arabic (a menu of the three modes, and of the Bangla voices under them while
 * Bangla plays, [voice] checked), and the sleep timer: one quiet row.
 */
@Composable
fun ModeAndSleepRow(
    mode: RecitationMode,
    voice: BanglaVoice,
    sleepTimer: SleepTimerStatus,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
) {
    // A flow row: with large text the sleep chip moves to a second line instead of squeezing the mode.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(QandeelTheme.spacing.sm, Alignment.CenterHorizontally),
    ) {
        ModeChip(mode, voice, actions.onModeChange, actions.onVoiceChange)
        SleepChip(sleepTimer, actions.onSleepTimerChange)
    }
}

@Composable
private fun ModeChip(
    mode: RecitationMode,
    voice: BanglaVoice,
    onModeChange: (RecitationMode) -> Unit,
    onVoiceChange: (BanglaVoice) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val description = stringResource(R.string.player_cd_mode_chip, modeName(mode))
    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text(modeName(mode), maxLines = 1) },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
            modifier = Modifier
                .testTag("player_mode")
                .semantics { contentDescription = description },
        )
        QandeelMenu(expanded = open, onDismissRequest = { open = false }) {
            RecitationMode.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(modeName(option)) },
                    trailingIcon = { if (option == mode) Icon(Icons.Rounded.Check, contentDescription = null) },
                    onClick = {
                        open = false
                        if (option != mode) onModeChange(option)
                    },
                )
            }
            // The voices only matter while Bangla plays.
            if (mode == RecitationMode.ARABIC_BANGLA) {
                HorizontalDivider(modifier = Modifier.padding(vertical = QandeelTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.bangla_voice),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = QandeelTheme.spacing.lg,
                        end = QandeelTheme.spacing.lg,
                        bottom = QandeelTheme.spacing.xs,
                    ),
                )
                BanglaVoice.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(banglaVoiceName(option)) },
                        trailingIcon = { if (option == voice) Icon(Icons.Rounded.Check, contentDescription = null) },
                        onClick = {
                            open = false
                            if (option != voice) onVoiceChange(option)
                        },
                    )
                }
            }
        }
    }
}
