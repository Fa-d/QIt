package dev.sadakat.qit.wear.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.*
import dev.sadakat.qit.wear.presentation.components.ConnectionStatus
import dev.sadakat.qit.wear.presentation.components.VolumeControl
import dev.sadakat.qit.wear.presentation.viewmodel.PlaybackViewModel
import dev.sadakat.qit.wear.playback.PlaybackManager
import dev.sadakat.qit.wear.presentation.model.ConnectionState
import dev.sadakat.qit.wear.presentation.model.StreamingMode
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    viewModel: PlaybackViewModel = hiltViewModel(),
    onBrowseMusicClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {}
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val streamingMode by viewModel.streamingMode.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val bufferingProgress by viewModel.bufferingProgress.collectAsState()
    val phoneBatteryLevel by viewModel.phoneBatteryLevel.collectAsState()

    val scalingLazyListState = rememberScalingLazyListState()

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            viewModel.updatePlaybackPosition()
            delay(1000)
        }
    }

    Scaffold(
        positionIndicator = {
            PositionIndicator(scalingLazyListState = scalingLazyListState)
        }
    ) {
        ScalingLazyColumn(
            state = scalingLazyListState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                vertical = 8.dp,
                horizontal = 16.dp
            )
        ) {
        // Connection Status
        item {
            ConnectionStatus(
                connectionState = connectionState,
                streamingMode = streamingMode,
                batteryLevel = phoneBatteryLevel,
                onRetry = { viewModel.retryConnection() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Title
        item {
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Buffering indicator (if buffering)
        if (isBuffering) {
            item {
                ConnectionStatus(
                    connectionState = ConnectionState.Connecting,
                    streamingMode = streamingMode,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Song Info
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentSong?.title ?: "No song playing",
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                currentSong?.artist?.let { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Time display
        item {
            Text(
                text = formatTime(currentPosition) + " / " + formatTime(duration),
                style = MaterialTheme.typography.caption1
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Volume Control
        item {
            VolumeControl(
                volume = volume,
                onVolumeChange = { viewModel.setVolume(it) },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Playback Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.skipToPrevious() },
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous"
                    )
                }

                Button(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isBuffering) {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colors.primary
                        }
                    )
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            indicatorColor = MaterialTheme.colors.onSurface,
                            trackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                    } else {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        )
                    }
                }

                Button(
                    onClick = { viewModel.skipToNext() },
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next"
                    )
                }
            }
        }

        // Streaming/Offline indicator at bottom
        if (streamingMode != StreamingMode.Unknown) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val icon = when (streamingMode) {
                        StreamingMode.Streaming -> Icons.Default.Wifi
                        StreamingMode.Offline -> Icons.Default.DownloadDone
                        StreamingMode.Unknown -> Icons.Default.WifiOff
                    }
                    val text = when (streamingMode) {
                        StreamingMode.Streaming -> "Streaming from phone"
                        StreamingMode.Offline -> "Playing from watch"
                        StreamingMode.Unknown -> "Unknown source"
                    }

                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.caption2
                    )
                }
            }
        }

        // Navigation buttons
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Browse Music button
        item {
            Chip(
                label = { Text("Browse Music") },
                onClick = onBrowseMusicClick,
                modifier = Modifier.fillMaxWidth(),
                icon = {
                    Icon(
                        Icons.Default.LibraryMusic,
                        contentDescription = "Browse Music"
                    )
                },
                colors = ChipDefaults.secondaryChipColors()
            )
        }

        // Downloads button (optional, for offline mode)
        item {
            Chip(
                label = { Text("Downloads") },
                onClick = onDownloadsClick,
                modifier = Modifier.fillMaxWidth(),
                icon = {
                    Icon(
                        Icons.Default.DownloadDone,
                        contentDescription = "Downloads"
                    )
                },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    if (milliseconds <= 0) return "0:00"
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
