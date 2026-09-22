package dev.sadakat.qit.presentation.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.designsystem.component.PlayerTokens
import dev.sadakat.qit.core.domain.player.PlaybackProgress
import dev.sadakat.qit.presentation.components.NumberBadge
import dev.sadakat.qit.presentation.components.ayahTitleText
import dev.sadakat.qit.presentation.components.bismillahTitleText

/**
 * The player pinned under every screen while something is queued: what plays, how far through the
 * surah (the line on top, read at draw time as it moves), play/pause and next. Tap or swipe it up
 * for the full player. There is no close button here — stopping is rare and lives in the full player.
 */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    progress: () -> PlaybackProgress,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nowPlaying = state.nowPlaying ?: return
    val surahName = state.surahName ?: stringResource(R.string.surah_fallback_name, nowPlaying.surah)
    val title = if (nowPlaying.ayah == 0) {
        bismillahTitleText(surahName)
    } else {
        ayahTitleText(surahName, nowPlaying.surah, nowPlaying.ayah)
    }
    val position =
        stringResource(R.string.player_position, nowPlaying.ayah, nowPlaying.ayahCount, nowPlaying.mode.label)
    val subtitle = listOfNotNull(position, sleepLabel(state.sleepTimer)).joinToString(" · ")
    val swipeUpPx = with(LocalDensity.current) { QItTheme.spacing.xxl.toPx() }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        // The surface runs behind the navigation bar (edge to edge); the controls stay above it.
        Column(Modifier.navigationBarsPadding()) {
            LinearProgressIndicator(
                // The surah's time once its length is known, else how many ayahs are behind.
                progress = {
                    progress().takeIf { it.surahDurationMs > 0 }?.fraction ?: nowPlaying.progress
                },
                color = MaterialTheme.colorScheme.primary,
                trackColor = QItTheme.colors.progressTrack,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlayerTokens.ProgressLineHeight),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlayerTokens.MiniPlayerHeight)
                    .clickable(onClickLabel = stringResource(R.string.player_cd_open), onClick = onExpand)
                    .pointerInput(onExpand) {
                        var dragged = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragged = 0f },
                            onVerticalDrag = { _, dy -> dragged += dy },
                            onDragEnd = { if (dragged < -swipeUpPx) onExpand() },
                        )
                    }
                    .padding(start = QItTheme.spacing.lg, end = QItTheme.spacing.xs)
                    .testTag("mini_player"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberBadge(nowPlaying.surah, size = QItTheme.sizes.numberBadgeSmall)
                Spacer(Modifier.width(QItTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (nowPlaying.isBuffering) {
                    val bufferingText = stringResource(R.string.cd_buffering)
                    CircularProgressIndicator(
                        strokeWidth = QItTheme.sizes.strokeThin,
                        modifier = Modifier
                            .size(QItTheme.sizes.iconSmall)
                            .testTag("player_buffering")
                            .semantics { contentDescription = bufferingText },
                    )
                }
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(
                            if (nowPlaying.isPlaying) R.string.cd_pause else R.string.cd_play,
                        ),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.cd_next_ayah))
                }
            }
        }
    }
}
