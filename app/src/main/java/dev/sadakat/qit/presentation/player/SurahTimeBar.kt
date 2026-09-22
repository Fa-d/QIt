package dev.sadakat.qit.presentation.player

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.DpSize
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.designsystem.component.PlayerTokens
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.PlaybackProgress
import kotlin.math.roundToLong

/**
 * Where in the surah playback is, as one recording: elapsed time on the left, time left on the
 * right. Dragging moves anywhere in the surah — into the middle of an ayah, too — and names the
 * ayah under the thumb ([ayahAt]) while dragging. Until the surah's length is known (the first
 * moments of a new queue) it only shows how many ayahs are behind.
 */
@Composable
fun SurahTimeBar(
    nowPlaying: NowPlaying,
    progress: () -> PlaybackProgress,
    ayahAt: (Long) -> Int?,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = progress()
    if (current.surahDurationMs > 0) {
        TimeSlider(current, ayahAt, onSeek, modifier)
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .padding(top = QItTheme.spacing.md)
                .fillMaxWidth()
                .height(QItTheme.sizes.touchTarget),
        ) {
            LinearProgressIndicator(
                progress = { nowPlaying.progress },
                trackColor = QItTheme.colors.progressTrack,
                gapSize = QItTheme.spacing.none,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlayerTokens.TimeBarTrackHeight),
            )
        }
    }
}

@Composable
private fun TimeSlider(
    progress: PlaybackProgress,
    ayahAt: (Long) -> Int?,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragged by remember { mutableStateOf<Float?>(null) }
    val shownMs = dragged?.roundToLong() ?: progress.surahPositionMs
    val elapsed = clockText(shownMs)
    val total = clockText(progress.surahDurationMs)
    val barDescription = stringResource(R.string.player_cd_time_bar)
    val barState = stringResource(R.string.player_time_state, elapsed, total)
    Column(modifier.padding(top = QItTheme.spacing.md)) {
        ThinSlider(
            value = dragged ?: progress.surahPositionMs.toFloat(),
            onValueChange = { dragged = it },
            onValueChangeFinished = {
                dragged?.let { onSeek(it.roundToLong()) }
                dragged = null
            },
            valueRange = 0f..progress.surahDurationMs.toFloat(),
            modifier = Modifier.semantics {
                contentDescription = barDescription
                stateDescription = barState
            },
        )
        Box(Modifier.fillMaxWidth()) {
            Text(
                elapsed,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            dragged?.let { ayahAt(it.roundToLong()) }?.takeIf { it > 0 }?.let { ayah ->
                Text(
                    text = stringResource(R.string.player_seek_ayah, ayah),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("player_seek_ayah"),
                )
            }
            Text(
                text = stringResource(R.string.player_time_remaining, clockText(progress.surahDurationMs - shownMs)),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/** A quiet slider: a thin track, no gap or stop dot, and a slim thumb. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Slider(
        value = value.coerceIn(valueRange),
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        interactionSource = interaction,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interaction,
                thumbSize = DpSize(PlayerTokens.TimeBarThumbWidth, PlayerTokens.TimeBarThumbHeight),
            )
        },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                thumbTrackGapSize = QItTheme.spacing.none,
                drawStopIndicator = null,
                colors = SliderDefaults.colors(inactiveTrackColor = QItTheme.colors.progressTrack),
                modifier = Modifier.height(PlayerTokens.TimeBarTrackHeight),
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("player_time_bar"),
    )
}
