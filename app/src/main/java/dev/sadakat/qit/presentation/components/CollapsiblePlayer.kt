package dev.sadakat.qit.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
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
            val floatingGap = 8.dp // Gap above bottom nav when collapsed
            val availableHeightPx = with(density) { availableHeight.toPx() }
            val collapsedHeightPx = with(density) { collapsedHeight.toPx() }
            val floatingGapPx = with(density) { floatingGap.toPx() }

            // Calculate base offset based on sheet state
            // When collapsed, leave gap above bottom nav for floating effect
            val baseOffset = when (playerSheetState) {
                PlayerSheetValue.Hidden -> availableHeightPx
                PlayerSheetValue.Collapsed -> availableHeightPx - collapsedHeightPx - floatingGapPx
                PlayerSheetValue.Expanded -> 0f
            }

            // Combine base offset with drag offset
            val targetOffset = if (isDragging) {
                (baseOffset + dragOffset).coerceIn(0f, availableHeightPx - collapsedHeightPx - floatingGapPx)
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
                            val collapsedOffset = availableHeightPx - collapsedHeightPx - floatingGapPx
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
            // Calculate continuous expansion progress (0f = collapsed, 1f = expanded)
            val collapsedOffset = availableHeightPx - collapsedHeightPx
            val progress = ((collapsedOffset - animatedOffset) / collapsedOffset).coerceIn(0f, 1f)

            // Morphing player content
            MorphingPlayerContent(
                song = currentSong!!,
                expansionProgress = progress,
                viewModel = viewModel,
                maxWidth = this@BoxWithConstraints.maxWidth,
                maxHeight = this@BoxWithConstraints.maxHeight,
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
 * Background layer that transitions from card to fullscreen with blurred album art
 */
@Composable
private fun BackgroundLayer(
    song: Song,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Blurred album art background (fades in)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.coverArtUri ?: "")
                .crossfade(1000)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.2f * progress
        )

        // Gradient overlay (fades in)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f * progress),
                            Color.Black.copy(alpha = 0.95f * progress)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
    }
}

/**
 * Morphing album art that transitions from small thumbnail to large centered square
 */
@Composable
private fun MorphingAlbumArt(
    albumArtUrl: String?,
    progress: Float,
    maxWidth: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Interpolate size: 48dp → 70% of screen width
    val collapsedSize = 48.dp
    val expandedSizePx = with(density) { maxWidth.toPx() * 0.7f }
    val albumArtSize = with(density) {
        lerp(collapsedSize.toPx(), expandedSizePx, progress).toDp()
    }

    // Interpolate corner radius: 8dp → 16dp
    val cornerRadius = with(density) {
        lerp(8.dp.toPx(), 16.dp.toPx(), progress).toDp()
    }

    // Interpolate elevation: 0dp → 8dp
    val elevation = with(density) {
        lerp(0.dp.toPx(), 8.dp.toPx(), progress).toDp()
    }

    Card(
        modifier = modifier.size(albumArtSize),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
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

/**
 * Morphing song info that transitions from left-aligned small text to centered large text
 */
@Composable
private fun MorphingSongInfo(
    song: Song,
    progress: Float,
    modifier: Modifier = Modifier
) {
    // Interpolate font sizes
    val titleFontSize = lerp(16f, 24f, progress).sp
    val artistFontSize = lerp(14f, 16f, progress).sp

    // Determine max lines based on progress
    val titleMaxLines = if (progress > 0.3f) 2 else 1

    // Text alignment: left → center
    val textAlign = if (progress > 0.5f) TextAlign.Center else TextAlign.Start

    // Spacing interpolation
    val spacing = with(LocalDensity.current) {
        lerp(2.dp.toPx(), 4.dp.toPx(), progress).toDp()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = if (progress > 0.5f) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (progress > 0.5f) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = titleFontSize
            ),
            textAlign = textAlign,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = song.artist ?: "",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = artistFontSize
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Morphing progress bar that transitions from thin indicator to full slider with time labels
 */
@Composable
private fun MorphingProgressBar(
    playbackProgress: Float,
    expansionProgress: Float,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier
) {
    var isUserInteracting by remember { mutableStateOf(false) }
    var userProgress by remember { mutableFloatStateOf(playbackProgress) }
    val density = LocalDensity.current

    // Animate progress smoothly between updates
    val animatedProgress by animateFloatAsState(
        targetValue = if (isUserInteracting) userProgress else playbackProgress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        ),
        label = "progress_animation"
    )

    // Update user progress when not interacting
    LaunchedEffect(playbackProgress, isUserInteracting) {
        if (!isUserInteracting) {
            userProgress = playbackProgress
        }
    }

    // Time labels alpha: fade in from 50% to 100%
    val timeLabelAlpha = ((expansionProgress - 0.5f) * 2f).coerceIn(0f, 1f)

    // Spacing interpolation
    val spacing = with(density) {
        lerp(0.dp.toPx(), 8.dp.toPx(), expansionProgress).toDp()
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Use Slider for both states, but morph its appearance
        Slider(
            value = if (isUserInteracting) userProgress else animatedProgress,
            onValueChange = {
                if (expansionProgress > 0.3f) { // Only allow interaction when partially expanded
                    isUserInteracting = true
                    userProgress = it
                }
            },
            onValueChangeFinished = {
                isUserInteracting = false
                onSeek((userProgress * duration).toLong())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { lerp(2.dp.toPx(), 48.dp.toPx(), expansionProgress).toDp() }),
            enabled = expansionProgress > 0.3f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = expansionProgress),
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                disabledThumbColor = Color.Transparent,
                disabledActiveTrackColor = MaterialTheme.colorScheme.primary,
                disabledInactiveTrackColor = Color.Transparent
            )
        )

        // Time labels (fade in when expanded)
        if (timeLabelAlpha > 0f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = timeLabelAlpha },
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
}

/**
 * Morphing playback controls with size interpolation and button fade-ins
 */
@Composable
private fun MorphingControls(
    isPlaying: Boolean,
    expansionProgress: Float,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Play button size: 48dp → 72dp
    val playButtonSize = with(density) { lerp(48.dp.toPx(), 72.dp.toPx(), expansionProgress).toDp() }
    val playIconSize = with(density) { lerp(24.dp.toPx(), 36.dp.toPx(), expansionProgress).toDp() }

    // Skip buttons size and icon: 48dp with icon 24dp → 28dp
    val skipButtonSize = 48.dp
    val skipIconSize = with(density) { lerp(24.dp.toPx(), 28.dp.toPx(), expansionProgress).toDp() }

    // Previous button alpha: fade in with expansion
    val previousButtonAlpha = expansionProgress

    // Shuffle/Repeat alpha: fade in from 70% onwards
    val shuffleRepeatAlpha = ((expansionProgress - 0.7f) / 0.3f).coerceIn(0f, 1f)

    // Play button background alpha
    val playButtonBackgroundAlpha = expansionProgress

    Row(
        modifier = modifier.height(72.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button (fade in from 70%)
        if (shuffleRepeatAlpha > 0f) {
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier
                    .size(skipButtonSize)
                    .graphicsLayer {
                        alpha = shuffleRepeatAlpha
                        scaleX = lerp(0.8f, 1f, shuffleRepeatAlpha)
                        scaleY = lerp(0.8f, 1f, shuffleRepeatAlpha)
                    }
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
        }

        // Previous Button (fade in)
        if (previousButtonAlpha > 0f) {
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier
                    .size(skipButtonSize)
                    .graphicsLayer { alpha = previousButtonAlpha }
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(skipIconSize),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Play/Pause Button (always visible, morphs size and background)
        if (playButtonBackgroundAlpha > 0.5f) {
            // Filled button when expanded
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(playButtonSize),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = playButtonBackgroundAlpha),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(playIconSize)
                )
            }
        } else {
            // Regular icon button when collapsed
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(playButtonSize)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(playIconSize)
                )
            }
        }

        // Next Button (always visible)
        IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(skipButtonSize)
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(skipIconSize)
            )
        }

        // Repeat Button (fade in from 70%)
        if (shuffleRepeatAlpha > 0f) {
            IconButton(
                onClick = onToggleRepeat,
                modifier = Modifier
                    .size(skipButtonSize)
                    .graphicsLayer {
                        alpha = shuffleRepeatAlpha
                        scaleX = lerp(0.8f, 1f, shuffleRepeatAlpha)
                        scaleY = lerp(0.8f, 1f, shuffleRepeatAlpha)
                    }
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
}

/**
 * Morphing player content that smoothly transitions all elements based on expansion progress
 * Uses absolute positioning to avoid layout structure changes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MorphingPlayerContent(
    song: Song,
    expansionProgress: Float,
    viewModel: PlayerViewModel,
    maxWidth: Dp,
    maxHeight: Dp,
    onTapCollapsed: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleMode by viewModel.shuffleMode.collectAsStateWithLifecycle()
    val progress by viewModel.progress
    val currentPosition by viewModel.currentPosition
    val duration by viewModel.duration
    val density = LocalDensity.current

    // Card properties that morph to fullscreen
    val cardElevation = with(density) { lerp(8.dp.toPx(), 0.dp.toPx(), expansionProgress).toDp() }
    val cardCornerRadius = with(density) { lerp(12.dp.toPx(), 0.dp.toPx(), expansionProgress).toDp() }
    // Mini player floats from bottom with minimal horizontal padding
    val cardHorizontalPadding = with(density) { lerp(8.dp.toPx(), 0.dp.toPx(), expansionProgress).toDp() }
    val cardVerticalPadding = with(density) { lerp(8.dp.toPx(), 0.dp.toPx(), expansionProgress).toDp() }

    // Top app bar alpha: fade in from 50%
    val topBarAlpha = ((expansionProgress - 0.5f) * 2f).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Background layer (card → blurred fullscreen)
        BackgroundLayer(
            song = song,
            progress = expansionProgress,
            modifier = Modifier.fillMaxSize()
        )

        // Main content card/container (just background)
        Card(
            onClick = if (expansionProgress < 0.3f) onTapCollapsed else ({}),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = cardHorizontalPadding,
                    vertical = cardVerticalPadding
                ),
            shape = RoundedCornerShape(cardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 1f - expansionProgress
                )
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = cardElevation
            )
        ) {
            // Empty card, just for background
            Box(modifier = Modifier.fillMaxSize())
        }

        // Top app bar (fade in when expanded)
        if (topBarAlpha > 0f) {
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
                ),
                modifier = Modifier.graphicsLayer { alpha = topBarAlpha }
            )
        }

        // Single layout - all elements positioned with transforms
        // This eliminates flickering by never switching layouts
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = cardHorizontalPadding,
                    vertical = cardVerticalPadding
                )
        ) {
            // Progress bar - morphs position from top to bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        val targetY = if (expansionProgress < 0.5f) {
                            0f
                        } else {
                            with(density) { (maxHeight.toPx() - 280.dp.toPx()) }
                        }
                        translationY = lerp(0f, targetY, expansionProgress)
                    }
            ) {
                MorphingProgressBar(
                    playbackProgress = progress,
                    expansionProgress = expansionProgress,
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = { position -> viewModel.seekTo(position) },
                    formatTime = { viewModel.formatTime(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Album art - single instance, transforms from left to center
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Collapsed: left at 8dp, centered vertically in mini player
                        // Expanded: centered horizontally, top at ~150dp
                        val collapsedX = with(density) { 8.dp.toPx() }
                        val expandedX = with(density) { (maxWidth.toPx() - (maxWidth.toPx() * 0.7f)) / 2f }
                        val targetX = lerp(collapsedX, expandedX, expansionProgress)

                        val collapsedY = with(density) { 16.dp.toPx() } // Below progress bar in mini player
                        val expandedY = with(density) { 120.dp.toPx() }
                        val targetY = lerp(collapsedY, expandedY, expansionProgress)

                        translationX = targetX
                        translationY = targetY
                    }
            ) {
                MorphingAlbumArt(
                    albumArtUrl = song.coverArtUri,
                    progress = expansionProgress,
                    maxWidth = maxWidth
                )
            }

            // Song info - transforms position and fades during transition
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        // Collapsed: next to album art in mini player
                        // Expanded: below album art
                        val collapsedX = with(density) { 64.dp.toPx() } // Right of album art
                        val expandedX = 0f
                        val targetX = lerp(collapsedX, expandedX, expansionProgress)

                        val collapsedY = with(density) { 28.dp.toPx() } // Vertically centered in mini player
                        val albumArtSize = with(density) {
                            lerp(48.dp.toPx(), maxWidth.toPx() * 0.7f, expansionProgress)
                        }
                        val expandedY = with(density) { 120.dp.toPx() + albumArtSize + 32.dp.toPx() }
                        val targetY = lerp(collapsedY, expandedY, expansionProgress)

                        translationX = targetX
                        translationY = targetY

                        // Fade slightly during mid-transition to reduce visual conflict
                        alpha = if (expansionProgress in 0.35f..0.65f) {
                            lerp(1f, 0.4f, (expansionProgress - 0.35f) / 0.3f).coerceIn(0.4f, 1f)
                        } else 1f
                    }
            ) {
                MorphingSongInfo(
                    song = song,
                    progress = expansionProgress,
                    modifier = Modifier.fillMaxWidth(if (expansionProgress < 0.5f) 0.35f else 1f)
                )
            }

            // Controls - transform from right side to bottom center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val collapsedY = with(density) { 22.dp.toPx() } // Centered in mini player
                        val expandedY = with(density) { maxHeight.toPx() - 200.dp.toPx() }
                        val targetY = lerp(collapsedY, expandedY, expansionProgress)

                        translationY = targetY
                    }
            ) {
                MorphingControls(
                    isPlaying = isPlaying,
                    expansionProgress = expansionProgress,
                    repeatMode = repeatMode,
                    shuffleMode = shuffleMode,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onSkipPrevious = { viewModel.skipToPrevious() },
                    onSkipNext = { viewModel.skipToNext() },
                    onToggleRepeat = { viewModel.toggleRepeatMode() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    modifier = if (expansionProgress < 0.5f) {
                        Modifier.align(Alignment.CenterEnd).wrapContentWidth().padding(end = 8.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
            }
        }
    }
}
