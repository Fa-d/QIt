what# Streaming UI Example

This document provides example UI code for consuming the streaming playback states.

## Composable Example

### Complete Playback Screen with Streaming Support

```kotlin
@Composable
fun PlaybackScreen(
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val bufferingProgress by viewModel.bufferingProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val streamingState by viewModel.streamingPlaybackState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Main playback UI
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Album art
            currentSong?.let { song ->
                AlbumArt(
                    imageUrl = song.albumArt,
                    modifier = Modifier.size(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Song info
            currentSong?.let { song ->
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.skipToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous")
                }

                IconButton(onClick = { viewModel.togglePlayPause() }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (isPlaying) "Pause" else "Play"
                    )
                }

                IconButton(onClick = { viewModel.skipToNext() }) {
                    Icon(Icons.Default.SkipNext, "Next")
                }
            }

            // Streaming status indicator
            StreamingStatusIndicator(
                streamingState = streamingState,
                isBuffering = isBuffering,
                bufferingProgress = bufferingProgress
            )
        }

        // Buffering overlay
        if (isBuffering) {
            BufferingOverlay(progress = bufferingProgress)
        }

        // Error overlay
        errorMessage?.let { error ->
            ErrorOverlay(
                message = error,
                onRetry = { viewModel.retryStreaming() },
                onDismiss = { viewModel.clearError() }
            )
        }
    }
}

@Composable
fun StreamingStatusIndicator(
    streamingState: StreamingPlaybackState,
    isBuffering: Boolean,
    bufferingProgress: Float
) {
    when (streamingState) {
        is StreamingPlaybackState.Idle -> {
            // No streaming active
        }
        is StreamingPlaybackState.Buffering -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                CircularProgressIndicator(
                    progress = streamingState.progress,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Buffering... ${(streamingState.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        is StreamingPlaybackState.Ready -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = "Streaming",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Streaming",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        is StreamingPlaybackState.Playing -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    Icons.Default.CloudQueue,
                    contentDescription = "Streaming",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Streaming from phone",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        is StreamingPlaybackState.Error -> {
            // Error handled by ErrorOverlay
        }
    }
}

@Composable
fun BufferingOverlay(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Buffering... ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}

@Composable
fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Playback Error",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
```

## Minimal Now Playing Widget

```kotlin
@Composable
fun NowPlayingWidget(
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val streamingState by viewModel.streamingPlaybackState.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art
            currentSong?.albumArt?.let { art ->
                AsyncImage(
                    model = art,
                    contentDescription = "Album art",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } ?: Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(4.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong?.title ?: "No song playing",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentSong?.artist ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Show streaming indicator
                    if (streamingState is StreamingPlaybackState.Playing ||
                        streamingState is StreamingPlaybackState.Buffering) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = "Streaming",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play/pause button
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
            }
        }
    }
}
```

## Connection Status Banner

```kotlin
@Composable
fun ConnectionStatusBanner(
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val errorMessage by viewModel.errorMessage.collectAsState()
    val streamingState by viewModel.streamingPlaybackState.collectAsState()

    AnimatedVisibility(
        visible = streamingState is StreamingPlaybackState.Error,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = "Connection lost",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Connection Lost",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = errorMessage ?: "Unable to stream from phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                TextButton(onClick = { viewModel.retryStreaming() }) {
                    Text("Retry")
                }
            }
        }
    }
}
```

## Buffering Progress Bar

```kotlin
@Composable
fun BufferingProgressBar(
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val isBuffering by viewModel.isBuffering.collectAsState()
    val bufferingProgress by viewModel.bufferingProgress.collectAsState()

    AnimatedVisibility(
        visible = isBuffering,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column {
            LinearProgressIndicator(
                progress = bufferingProgress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Buffering stream... ${(bufferingProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
```

## Song List Item with Streaming Badge

```kotlin
@Composable
fun SongListItem(
    song: Song,
    isCurrentlyPlaying: Boolean,
    isStreaming: Boolean,
    onSongClick: (String) -> Unit
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(song.title)
                if (isCurrentlyPlaying && isStreaming) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Text("Streaming", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        supportingContent = { Text(song.artist) },
        leadingContent = {
            if (song.albumArt != null) {
                AsyncImage(
                    model = song.albumArt,
                    contentDescription = "Album art",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        trailingContent = {
            if (song.isAvailableOnWatch()) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = "Requires streaming",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        modifier = Modifier.clickable { onSongClick(song.id.value) }
    )
}
```

## Usage in Activity/Fragment

```kotlin
class PlaybackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QItTheme {
                Scaffold { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues)) {
                        // Connection status at top
                        ConnectionStatusBanner()

                        // Main playback screen
                        PlaybackScreen()
                    }
                }
            }
        }
    }
}
```

## Toast/Snackbar for Quick Feedback

```kotlin
@Composable
fun StreamingFeedback(
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by viewModel.errorMessage.collectAsState()
    val streamingState by viewModel.streamingPlaybackState.collectAsState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Retry",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.retryStreaming()
            }
        }
    }

    LaunchedEffect(streamingState) {
        if (streamingState is StreamingPlaybackState.Ready) {
            snackbarHostState.showSnackbar(
                message = "Streaming ready",
                duration = SnackbarDuration.Short
            )
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}
```

## Advanced: Retry with Exponential Backoff

```kotlin
@Composable
fun SmartRetryButton(
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    var retryCount by remember { mutableStateOf(0) }
    var isRetrying by remember { mutableStateOf(false) }
    val errorMessage by viewModel.errorMessage.collectAsState()

    fun calculateBackoff(count: Int): Long {
        return (1000L * 2.0.pow(count.toDouble())).toLong().coerceAtMost(30000L)
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage == null) {
            retryCount = 0
            isRetrying = false
        }
    }

    if (errorMessage != null) {
        Button(
            onClick = {
                isRetrying = true
                viewModel.retryStreaming()
                retryCount++
            },
            enabled = !isRetrying
        ) {
            if (isRetrying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                if (retryCount == 0) "Retry"
                else "Retry (${retryCount})"
            )
        }

        if (retryCount > 0) {
            Text(
                text = "Next retry in ${calculateBackoff(retryCount - 1) / 1000}s",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

## Summary

These UI examples demonstrate:

1. **Comprehensive State Handling**: All streaming states are properly displayed
2. **User Feedback**: Clear indicators for buffering, errors, and streaming status
3. **Recovery Options**: Retry buttons and error overlays
4. **Visual Indicators**: Icons, badges, and progress bars for streaming
5. **Responsive UI**: Loading states prevent user confusion
6. **WearOS Optimized**: Compact designs suitable for small screens

Use these examples as templates for implementing streaming-aware UI in your WearOS app.
