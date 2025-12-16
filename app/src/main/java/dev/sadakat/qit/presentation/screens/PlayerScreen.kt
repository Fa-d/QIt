package dev.sadakat.qit.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sadakat.qit.presentation.components.PlaybackQueue
import dev.sadakat.qit.presentation.viewmodel.PlayerViewModel
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.valueobject.RepeatMode
import dev.sadakat.qit.shared.domain.valueobject.ShuffleMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()
    val progress by remember { derivedStateOf { viewModel.progress } }
    val currentPosition by remember { derivedStateOf { viewModel.currentPosition } }
    val duration by remember { derivedStateOf { viewModel.duration } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleQueueVisibility() }) {
                        Icon(Icons.Default.QueueMusic, contentDescription = "Queue")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (viewModel.showQueue) {
            PlaybackQueue(
                songs = viewModel.playbackQueue.value,
                currentIndex = viewModel.currentQueueIndex.value,
                onSongClick = { index -> viewModel.playQueueItem(index) },
                onRemove = { index -> viewModel.removeFromQueue(index) },
                onClose = { viewModel.toggleQueueVisibility() },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Album Art
                    AlbumArt(
                        albumArtUrl = currentSong?.coverArtUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    // Song Info
                    SongInfo(
                        song = currentSong,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Progress Bar
                    ProgressBar(
                        progress = progress,
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeek = { position -> viewModel.seekTo(position) },
                        formatTime = { viewModel.formatTime(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Playback Controls
                    PlaybackControls(
                        isPlaying = isPlaying,
                        repeatMode = repeatMode,
                        shuffleMode = shuffleMode,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onSkipPrevious = { viewModel.skipToPrevious() },
                        onSkipNext = { viewModel.skipToNext() },
                        onToggleRepeat = { viewModel.toggleRepeatMode() },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumArt(
    albumArtUrl: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(albumArtUrl ?: "")
                .crossfade(true)
                .build(),
            contentDescription = "Album Art",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun SongInfo(
    song: Song?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = song?.title ?: "No song playing",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = song?.artist ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = song?.album ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(progress) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Slider(
            value = if (isDragging) dragProgress else progress,
            onValueChange = {
                dragProgress = it
                isDragging = true
            },
            onValueChangeFinished = {
                onSeek((dragProgress * duration).toLong())
                isDragging = false
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top row: Shuffle and Repeat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleMode == ShuffleMode.ALL) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(48.dp)) // Balance the layout

            IconButton(onClick = onToggleRepeat) {
                val icon = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Outlined.RepeatOne
                    RepeatMode.ALL -> Icons.Default.Repeat
                    RepeatMode.NONE -> Icons.Default.Repeat
                }
                Icon(
                    icon,
                    contentDescription = "Repeat Mode",
                    tint = if (repeatMode != RepeatMode.NONE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // Main controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp)
                )
            }

            FloatingActionButton(
                onClick = onPlayPause,
                modifier = Modifier.size(80.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}