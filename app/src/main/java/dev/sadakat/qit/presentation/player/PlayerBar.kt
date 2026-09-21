package dev.sadakat.qit.presentation.player

// qit:legacy-ui — predates the design tokens; its UX slice replaces it.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sadakat.qit.R
import dev.sadakat.qit.presentation.components.ayahTitleText
import dev.sadakat.qit.presentation.components.bismillahTitleText

/** Persistent bar over the whole app showing what plays and its main controls. */
@Composable
fun PlayerBar(
    state: PlayerBarUiState,
    onOpenReader: (surah: Int, ayah: Int) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nowPlaying = state.nowPlaying ?: return
    val surahName = state.surahName
        ?: stringResource(R.string.surah_fallback_name, nowPlaying.surah)
    val title = if (nowPlaying.ayah == 0) {
        bismillahTitleText(surahName)
    } else {
        ayahTitleText(surahName, nowPlaying.surah, nowPlaying.ayah)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            // The app draws edge to edge: the bar's surface goes behind the system navigation bar,
            // its controls stay above it.
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                // Tapping what is playing opens the reader at that surah and ayah.
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenReader(nowPlaying.surah, nowPlaying.ayah) },
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = nowPlaying.mode.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (nowPlaying.isBuffering) {
                val bufferingText = stringResource(R.string.cd_buffering)
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                        .testTag("player_buffering")
                        .semantics { contentDescription = bufferingText },
                )
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous_ayah),
                )
            }
            IconButton(onClick = onTogglePlayPause) {
                if (nowPlaying.isPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = stringResource(R.string.cd_pause),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play),
                    )
                }
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.cd_next_ayah),
                )
            }
            IconButton(onClick = onStop) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close_player),
                )
            }
        }
    }
}
