package dev.sadakat.qandeel.wear.presentation.options

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.sadakat.qandeel.core.domain.player.PlaybackSpeed
import dev.sadakat.qandeel.wear.R
import kotlin.math.ceil

@Composable
fun OptionsRoute(modifier: Modifier = Modifier, viewModel: OptionsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OptionsScreen(
        uiState = uiState,
        onSelectSpeed = viewModel::selectSpeed,
        onSelectRepeat = viewModel::selectRepeat,
        onSelectSleep = viewModel::selectSleep,
        modifier = modifier,
    )
}

/** Speed, ayah repeat and the sleep timer, one radio group each. */
@Composable
fun OptionsScreen(
    uiState: WearOptionsUiState,
    onSelectSpeed: (PlaybackSpeed) -> Unit,
    onSelectRepeat: (RepeatSelection) -> Unit,
    onSelectSleep: (SleepSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "speed-header") { HeaderRow(stringResource(R.string.speed_header)) }
            PlaybackSpeed.entries.forEach { speed ->
                item(key = "speed-$speed") {
                    OptionRow(
                        label = speedLabel(speed),
                        selected = speed == uiState.speed,
                        onSelect = { onSelectSpeed(speed) },
                    )
                }
            }

            item(key = "repeat-header") { HeaderRow(stringResource(R.string.repeat_header)) }
            RepeatSelection.entries.forEach { selection ->
                item(key = "repeat-$selection") {
                    OptionRow(
                        label = repeatLabel(selection),
                        selected = selection == uiState.repeat,
                        onSelect = { onSelectRepeat(selection) },
                    )
                }
            }

            item(key = "sleep-header") {
                HeaderRow(stringResource(R.string.sleep_header), secondary = sleepSecondary(uiState))
            }
            SleepSelection.entries.forEach { selection ->
                item(key = "sleep-$selection") {
                    OptionRow(
                        label = sleepLabel(selection),
                        selected = selection == uiState.sleep,
                        onSelect = { onSelectSleep(selection) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.HeaderRow(title: String, secondary: String? = null) {
    val transformationSpec = rememberTransformationSpec()
    ListHeader(
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            // The running timer's countdown belongs with its heading, not on the options themselves.
            secondary?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.OptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    val transformationSpec = rememberTransformationSpec()
    RadioButton(
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec),
    ) {
        Text(label)
    }
}

@Composable
private fun speedLabel(speed: PlaybackSpeed): String = when (speed) {
    PlaybackSpeed.X0_75 -> stringResource(R.string.speed_0_75)
    PlaybackSpeed.X1 -> stringResource(R.string.speed_1)
    PlaybackSpeed.X1_25 -> stringResource(R.string.speed_1_25)
    PlaybackSpeed.X1_5 -> stringResource(R.string.speed_1_5)
}

@Composable
private fun repeatLabel(selection: RepeatSelection): String = when (selection) {
    RepeatSelection.OFF -> stringResource(R.string.repeat_off)
    RepeatSelection.THREE_TIMES -> stringResource(R.string.repeat_three_times)
    RepeatSelection.FOREVER -> stringResource(R.string.repeat_forever)
}

@Composable
private fun sleepLabel(selection: SleepSelection): String = when (selection) {
    SleepSelection.OFF -> stringResource(R.string.sleep_off)
    SleepSelection.MINUTES_15 -> stringResource(R.string.sleep_minutes, 15)
    SleepSelection.MINUTES_30 -> stringResource(R.string.sleep_minutes, 30)
    SleepSelection.MINUTES_60 -> stringResource(R.string.sleep_minutes, 60)
    SleepSelection.END_OF_SURAH -> stringResource(R.string.sleep_end_of_surah)
}

@Composable
private fun sleepSecondary(uiState: WearOptionsUiState): String? = when {
    uiState.sleepAtEndOfSurah -> stringResource(R.string.sleep_stops_at_surah_end)

    // Round up: "1 min left" until the last minute has fully passed.
    uiState.sleepRemainingMs != null -> stringResource(
        R.string.sleep_remaining,
        ceil(uiState.sleepRemainingMs / MS_PER_MINUTE).toInt().coerceAtLeast(1),
    )

    else -> null
}

private const val MS_PER_MINUTE = 60_000.0
