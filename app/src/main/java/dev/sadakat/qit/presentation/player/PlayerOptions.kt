package dev.sadakat.qit.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.SleepOption
import dev.sadakat.qit.core.domain.player.SleepTimerStatus
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Repeat, speed and sleep timer: three chips showing their current value, each opening its choices. */
@Composable
fun PlayerOptions(
    nowPlaying: NowPlaying,
    sleepTimer: SleepTimerStatus,
    onRepeatChange: (RepeatSetting) -> Unit,
    onSpeedChange: (PlaybackSpeed) -> Unit,
    onSleepTimerChange: (SleepOption?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var repeatDialogOpen by rememberSaveable { mutableStateOf(false) }
    // A flow row: with large text the chips move to a second line instead of wrapping their labels.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(QItTheme.spacing.sm, Alignment.CenterHorizontally),
    ) {
        FilterChip(
            selected = nowPlaying.repeat != RepeatSetting.Off,
            onClick = { repeatDialogOpen = true },
            label = { Text(repeatLabel(nowPlaying.repeat), maxLines = 1) },
            leadingIcon = { Icon(Icons.Rounded.Repeat, contentDescription = null) },
            modifier = Modifier.testTag("chip_repeat"),
        )
        SpeedChip(nowPlaying.speed, onSpeedChange)
        SleepChip(sleepTimer, onSleepTimerChange)
    }
    if (repeatDialogOpen) {
        RepeatDialog(
            current = nowPlaying.repeat,
            ayah = max(nowPlaying.ayah, 1),
            ayahCount = nowPlaying.ayahCount,
            onConfirm = {
                repeatDialogOpen = false
                onRepeatChange(it)
            },
            onDismiss = { repeatDialogOpen = false },
        )
    }
}

@Composable
private fun SpeedChip(speed: PlaybackSpeed, onSpeedChange: (PlaybackSpeed) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = speed != PlaybackSpeed.X1,
            onClick = { open = true },
            label = { Text(stringResource(R.string.player_speed, speedFactor(speed)), maxLines = 1) },
            leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
            modifier = Modifier.testTag("chip_speed"),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
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

@Composable
private fun SleepChip(status: SleepTimerStatus, onChange: (SleepOption?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val active = status != SleepTimerStatus.Off
    Box {
        FilterChip(
            selected = active,
            onClick = { open = true },
            label = { Text(sleepLabel(status) ?: stringResource(R.string.player_sleep), maxLines = 1) },
            leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null) },
            modifier = Modifier.testTag("chip_sleep"),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SLEEP_MINUTES.forEach { minutes ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.player_sleep_minutes, minutes)) },
                    onClick = {
                        open = false
                        onChange(SleepOption.Minutes(minutes))
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.player_sleep_end_of_surah)) },
                onClick = {
                    open = false
                    onChange(SleepOption.EndOfSurah)
                },
            )
            if (active) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.player_sleep_cancel)) },
                    onClick = {
                        open = false
                        onChange(null)
                    },
                )
            }
        }
    }
}

private enum class RepeatTarget { OFF, AYAH, RANGE }

/**
 * Choose what repeats: off, each ayah, or a range of this surah (drag both ends), and how many
 * times. Opening it while nothing repeats suggests "each ayah" — that's what memorizing usually needs.
 */
@Composable
fun RepeatDialog(
    current: RepeatSetting,
    ayah: Int,
    ayahCount: Int,
    onConfirm: (RepeatSetting) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var target by rememberSaveable { mutableStateOf(initialTarget(current)) }
    var from by rememberSaveable { mutableStateOf((current as? RepeatSetting.Range)?.from ?: ayah) }
    var to by rememberSaveable {
        mutableStateOf((current as? RepeatSetting.Range)?.to ?: min(ayah + DEFAULT_RANGE_EXTRA, ayahCount))
    }
    var times by rememberSaveable { mutableStateOf(initialTimes(current)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_repeat_title)) },
        text = {
            RepeatChoices(
                target = target,
                range = from..to,
                ayahCount = ayahCount,
                times = times,
                onTargetChange = { target = it },
                onRangeChange = {
                    from = it.first
                    to = it.last
                },
                onTimesChange = { times = it },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(repeatSetting(target, from, to, times)) }) {
                Text(stringResource(R.string.player_repeat_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.player_cancel)) }
        },
        modifier = modifier,
    )
}

@Composable
private fun RepeatChoices(
    target: RepeatTarget,
    range: IntRange,
    ayahCount: Int,
    times: Int?,
    onTargetChange: (RepeatTarget) -> Unit,
    onRangeChange: (IntRange) -> Unit,
    onTimesChange: (Int?) -> Unit,
) {
    Column {
        Segments(options = RepeatTarget.entries, selected = target, label = {
            targetLabel(it)
        }, onSelect = onTargetChange)
        if (target == RepeatTarget.RANGE) {
            Text(
                text = stringResource(R.string.player_repeat_range_label, range.first, range.last),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = QItTheme.spacing.lg),
            )
            RangeSlider(
                value = range.first.toFloat()..range.last.toFloat(),
                onValueChange = {
                    val first = it.start.roundToInt()
                    onRangeChange(first..max(it.endInclusive.roundToInt(), first))
                },
                valueRange = 1f..ayahCount.toFloat(),
                modifier = Modifier.testTag("repeat_range"),
            )
        }
        if (target != RepeatTarget.OFF) {
            Text(
                text = stringResource(R.string.player_repeat_times),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(top = QItTheme.spacing.lg, bottom = QItTheme.spacing.sm)
                    .semantics { heading() },
            )
            Segments(options = TIMES, selected = times, label = { timesLabel(it) }, onSelect = onTimesChange)
        }
    }
}

@Composable
private fun targetLabel(target: RepeatTarget): String = stringResource(
    when (target) {
        RepeatTarget.OFF -> R.string.player_repeat_target_off
        RepeatTarget.AYAH -> R.string.player_repeat_target_ayah
        RepeatTarget.RANGE -> R.string.player_repeat_target_range
    },
)

@Composable
private fun timesLabel(times: Int?): String = times?.let { stringResource(R.string.player_repeat_times_n, it) }
    ?: stringResource(R.string.player_repeat_times_forever)

private fun initialTarget(current: RepeatSetting) =
    if (current is RepeatSetting.Range) RepeatTarget.RANGE else RepeatTarget.AYAH

private fun initialTimes(current: RepeatSetting): Int? = when (current) {
    is RepeatSetting.Ayah -> current.times
    is RepeatSetting.Range -> current.times
    RepeatSetting.Off -> DEFAULT_TIMES
}

private fun repeatSetting(target: RepeatTarget, from: Int, to: Int, times: Int?): RepeatSetting = when (target) {
    RepeatTarget.OFF -> RepeatSetting.Off
    RepeatTarget.AYAH -> RepeatSetting.Ayah(times)
    RepeatTarget.RANGE -> RepeatSetting.Range(from, to, times)
}

@Composable
private fun <T> Segments(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {},
            ) {
                Text(label(option), maxLines = 1)
            }
        }
    }
}

private val SLEEP_MINUTES = listOf(15, 30, 45, 60)

/** Repeat counts offered; null is "forever". */
private val TIMES = listOf(2, 3, 5, null)
private const val DEFAULT_TIMES = 3

/** A new range starts at the current ayah and spans this many more (a typical memorizing chunk). */
private const val DEFAULT_RANGE_EXTRA = 4
