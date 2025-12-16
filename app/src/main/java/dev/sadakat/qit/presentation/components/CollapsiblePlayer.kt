package dev.sadakat.qit.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sadakat.qit.presentation.viewmodel.PlayerSheetValue
import dev.sadakat.qit.presentation.viewmodel.PlayerViewModel
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.valueobject.RepeatMode
import dev.sadakat.qit.shared.domain.valueobject.ShuffleMode
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsiblePlayer(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val playerSheetState by viewModel.playerSheetState.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    // Track drag offset
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Back button collapses when expanded
    BackHandler(enabled = playerSheetState == PlayerSheetValue.Expanded) {
        viewModel.collapsePlayer()
    }

    // Render player sheet
    if (currentSong != null) {
        BoxWithConstraints(
            modifier = modifier.fillMaxSize()
        ) {
            // Get actual available height
            val availableHeight = maxHeight
            val collapsedHeight = 80.dp
            val availableHeightPx = with(density) { availableHeight.toPx() }
            val collapsedHeightPx = with(density) { collapsedHeight.toPx() }

            // Calculate base offset based on sheet state
            val baseOffset = when (playerSheetState) {
                PlayerSheetValue.Hidden -> availableHeightPx
                PlayerSheetValue.Collapsed -> availableHeightPx - collapsedHeightPx
                PlayerSheetValue.Expanded -> 0f
            }

            // Combine base offset with drag offset
            val targetOffset = if (isDragging) {
                (baseOffset + dragOffset).coerceIn(0f, availableHeightPx - collapsedHeightPx)
            } else {
                baseOffset
            }

            // Animate offset smoothly when not dragging
            val animatedOffset by animateFloatAsState(
                targetValue = targetOffset,
                animationSpec = if (isDragging) snap() else tween(300, easing = EaseOutQuart),
                label = "player_offset"
            )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = with(density) { animatedOffset.toDp() })
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragOffset = 0f
                        },
                        onDragEnd = {
                            isDragging = false

                            // Calculate current position as percentage
                            val currentOffset = baseOffset + dragOffset
                            val collapsedOffset = availableHeightPx - collapsedHeightPx
                            val expandedOffset = 0f

                            // Determine which state to snap to based on position and velocity
                            when {
                                // If closer to expanded (top), expand
                                currentOffset < (collapsedOffset / 2f) -> {
                                    viewModel.expandPlayer()
                                }
                                // Otherwise collapse
                                else -> {
                                    viewModel.collapsePlayer()
                                }
                            }

                            dragOffset = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffset = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
                }
        ) {
            // Calculate expansion progress (0f = collapsed, 1f = expanded)
            val progress = when (playerSheetState) {
                PlayerSheetValue.Hidden -> 0f
                PlayerSheetValue.Collapsed -> 0f
                PlayerSheetValue.Expanded -> 1f
            }

            // Player content with morphing animation
            PlayerContent(
                song = currentSong!!,
                expansionProgress = progress,
                viewModel = viewModel,
                onTapCollapsed = { viewModel.expandPlayer() }
            )

            // Queue modal overlay
            if (viewModel.showQueue) {
                PlaybackQueue(
                    songs = viewModel.playbackQueue.value,
                    currentIndex = viewModel.currentQueueIndex.value,
                    onSongClick = { index -> viewModel.playQueueItem(index) },
                    onRemove = { index -> viewModel.removeFromQueue(index) },
                    onClose = { viewModel.toggleQueueVisibility() }
                )
            }
        }
        }
    }
}

/**
 * Player content that morphs between collapsed and expanded states
 */
@Composable
private fun PlayerContent(
    song: Song,
    expansionProgress: Float,
    viewModel: PlayerViewModel,
    onTapCollapsed: () -> Unit
) {
    // Use crossfade for clean transition at 50% threshold
    Crossfade(
        targetState = expansionProgress > 0.5f,
        animationSpec = tween(200),
        label = "player_content_crossfade"
    ) { isExpanded ->
        if (isExpanded) {
            ExpandedPlayerContent(
                song = song,
                viewModel = viewModel
            )
        } else {
            CollapsedPlayerContent(
                song = song,
                viewModel = viewModel,
                onTap = onTapCollapsed
            )
        }
    }
}

/**
 * Collapsed mini player content (extracted from MiniPlayer)
 */
@Composable
private fun CollapsedPlayerContent(
    song: Song,
    viewModel: PlayerViewModel,
    onTap: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress

    // Animate progress smoothly
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        ),
        label = "mini_player_progress"
    )

    Card(
        onClick = onTap,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column {
            // Thin progress indicator at top
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Album art
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.coverArtUri ?: "")
                        .crossfade(500)
                        .build(),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                // Song info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = song.artist ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause button
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Skip Next button
                    IconButton(
                        onClick = { viewModel.skipToNext() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Expanded full player content (extracted from PlayerScreen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedPlayerContent(
    song: Song,
    viewModel: PlayerViewModel
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleMode by viewModel.shuffleMode.collectAsStateWithLifecycle()
    val progress by viewModel.progress
    val currentPosition by viewModel.currentPosition
    val duration by viewModel.duration

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient background with blurred album art
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.coverArtUri ?: "")
                .crossfade(1000)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.2f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.collapsePlayer() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleQueueVisibility() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = "Queue",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { /* TODO: Add to favorites */ },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Album Art
                AlbumArt(
                    albumArtUrl = song.coverArtUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Song Info
                SongInfo(
                    song = song,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Progress Bar
                ProgressBar(
                    progress = progress,
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = { position -> viewModel.seekTo(position) },
                    formatTime = { viewModel.formatTime(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

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

                Spacer(modifier = Modifier.height(24.dp))
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(albumArtUrl ?: "")
                .crossfade(1000)
                .build(),
            contentDescription = "Album Art",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun SongInfo(
    song: Song,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = song.artist ?: "",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp
            ),
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
    var isUserInteracting by remember { mutableStateOf(false) }
    var userProgress by remember { mutableFloatStateOf(progress) }

    // Animate progress smoothly between updates
    val animatedProgress by animateFloatAsState(
        targetValue = if (isUserInteracting) userProgress else progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        ),
        label = "progress_animation"
    )

    // Update user progress when not interacting
    LaunchedEffect(progress, isUserInteracting) {
        if (!isUserInteracting) {
            userProgress = progress
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Slider(
            value = if (isUserInteracting) userProgress else animatedProgress,
            onValueChange = {
                isUserInteracting = true
                userProgress = it
            },
            onValueChangeFinished = {
                isUserInteracting = false
                onSeek((userProgress * duration).toLong())
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
    Row(
        modifier = modifier.height(72.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleMode == ShuffleMode.ALL) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(24.dp)
            )
        }

        // Previous Button
        IconButton(
            onClick = onSkipPrevious,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Play/Pause Button
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp)
            )
        }

        // Next Button
        IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Repeat Button
        IconButton(
            onClick = onToggleRepeat,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    RepeatMode.ALL -> Icons.Default.Repeat
                    RepeatMode.NONE -> Icons.Default.Repeat
                },
                contentDescription = "Repeat Mode",
                tint = if (repeatMode != RepeatMode.NONE) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
