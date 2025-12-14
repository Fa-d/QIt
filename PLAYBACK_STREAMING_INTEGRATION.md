# Playback Streaming Integration

This document describes the complete streaming-aware playback flow implemented in the QIt WearOS app.

## Overview

The playback system now supports both local and streamed playback with proper buffering state management, connection loss handling, and seamless mode switching.

## Architecture

### Key Components

1. **PlaybackManager** (`wear/src/main/java/dev/sadakat/qit/wear/playback/PlaybackManager.kt`)
   - Manages ExoPlayer for audio playback
   - Supports local and streaming playback modes
   - Monitors streaming status and handles buffering
   - Handles connection loss gracefully with retry support

2. **PlaySongUseCase** (`wear/src/main/java/dev/sadakat/qit/wear/application/usecase/playback/PlaySongUseCase.kt`)
   - Determines playback strategy (Local vs Streaming)
   - Integrates with StreamingCoordinator
   - Returns appropriate PlaybackSource

3. **PlaybackViewModel** (`wear/src/main/java/dev/sadakat/qit/wear/presentation/viewmodel/PlaybackViewModel.kt`)
   - Exposes buffering state to UI
   - Handles streaming errors
   - Provides retry functionality
   - Shows connection status

4. **StreamingCoordinator** (`shared/src/main/java/dev/sadakat/qit/shared/domain/service/StreamingCoordinator.kt`)
   - Determines optimal streaming strategy
   - Handles connection loss scenarios
   - Manages streaming lifecycle

## Playback Flow

### 1. User Selects a Song

```
User taps song → PlaybackViewModel.playSong(songId)
```

### 2. Strategy Determination

```kotlin
// PlaySongUseCase.invoke()
1. Get song from repository
2. Determine streaming strategy via StreamingCoordinator
   - If available on watch → StreamingStrategy.Local
   - If phone connected → StreamingStrategy.RealTime or Progressive
   - If unavailable → StreamingStrategy.Unavailable
```

### 3. Initiate Streaming (if needed)

```kotlin
// For streaming strategies
1. Initiate streaming request to phone
2. Wait for buffer to be ready
3. Return PlaybackSource.Streaming
```

### 4. Start Playback

```kotlin
// PlaybackViewModel routes to appropriate method
if (playbackSource is Local) {
    playLocalSong(song)
} else {
    playStreamedSong(song, strategy)
}
```

### 5. Monitor Streaming Status

```kotlin
// PlaybackManager monitors StreamingRepository
streamingRepository.observeStreamingStatus()
    .collect { status ->
        when (status) {
            Buffering(progress) → Update UI
            Error → Handle connection loss
        }
    }
```

## State Management

### StreamingPlaybackState

The system uses a comprehensive state model for streaming:

```kotlin
sealed class StreamingPlaybackState {
    object Idle : StreamingPlaybackState()
    data class Buffering(val progress: Float) : StreamingPlaybackState()
    data class Ready(val song: Song) : StreamingPlaybackState()
    data class Playing(val song: Song, val position: Long) : StreamingPlaybackState()
    data class Error(val message: String) : StreamingPlaybackState()
}
```

### UI State Exposure

The ViewModel exposes several state flows for the UI:

```kotlin
// Buffering state
val isBuffering: StateFlow<Boolean>
val bufferingProgress: StateFlow<Float>

// Error state
val errorMessage: StateFlow<String?>

// Streaming state
val streamingPlaybackState: StateFlow<StreamingPlaybackState>
```

## Connection Loss Handling

### Automatic Handling

When a connection loss is detected:

```kotlin
1. Pause playback immediately
2. Store last known position (lastKnownPosition)
3. Update state to StreamingPlaybackState.Error("Connection lost")
4. Stop streaming monitor
```

### User-Initiated Retry

User can retry streaming after connection loss:

```kotlin
// In UI
PlaybackViewModel.retryStreaming()

// Flow:
1. Clear error message
2. PlaybackManager.retryStreaming(streamUri)
3. Restore playback from last known position
4. Resume streaming monitor
```

## Mode Switching

The system supports switching between local and streamed playback:

```kotlin
PlaybackManager.switchPlaybackMode(song, mode, streamUri)

// Preserves:
- Current playback position
- Playing/paused state
```

## Buffering State Updates

### From ExoPlayer

```kotlin
Player.STATE_BUFFERING →
    StreamingPlaybackState.Buffering(progress)
```

### From StreamingRepository

```kotlin
StreamingStatus.Buffering(progress) →
    StreamingPlaybackState.Buffering(progress)
```

### Exposed to UI

```kotlin
viewModel.isBuffering.collect { isBuffering ->
    if (isBuffering) {
        showBufferingIndicator()
    }
}

viewModel.bufferingProgress.collect { progress ->
    updateBufferingProgress(progress)
}
```

## Error Handling

### Types of Errors

1. **Network Errors** - Connection loss during streaming
2. **Playback Errors** - ExoPlayer errors
3. **Strategy Errors** - Song unavailable for playback

### Error Flow

```kotlin
Error detected →
    StreamingPlaybackState.Error(message) →
    ViewModel.errorMessage →
    UI shows error and retry option
```

### Recovery Options

1. **Retry Streaming** - For temporary connection issues
2. **Switch to Different Song** - If retry fails
3. **Download for Offline** - If streaming consistently fails

## Backward Compatibility

Local playback continues to work without changes:

```kotlin
// Local playback path (unchanged)
playbackManager.playLocalSong(song)
- No streaming overhead
- No buffering states
- Direct file playback
```

## Dependency Injection

Updated PlaybackModule provides required dependencies:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    @PlaybackScope
    fun providePlaybackCoroutineScope(): CoroutineScope

    fun providePlaybackManager(
        context: Context,
        streamingRepository: StreamingRepository,
        coroutineScope: CoroutineScope
    ): PlaybackManager
}
```

## Usage Example

### In UI Code

```kotlin
// Observe streaming state
viewModel.streamingPlaybackState.collect { state ->
    when (state) {
        is StreamingPlaybackState.Idle -> {
            hideBuffering()
            hideError()
        }
        is StreamingPlaybackState.Buffering -> {
            showBuffering(state.progress)
        }
        is StreamingPlaybackState.Ready -> {
            hideBuffering()
            showReady()
        }
        is StreamingPlaybackState.Playing -> {
            updateNowPlaying(state.song)
        }
        is StreamingPlaybackState.Error -> {
            showError(state.message)
            showRetryButton()
        }
    }
}

// Handle retry
retryButton.onClick {
    viewModel.retryStreaming()
}
```

## Future Improvements

1. **Custom DataSource** - Implement custom ExoPlayer DataSource for better streaming control
2. **Adaptive Quality** - Automatically adjust quality based on connection
3. **Pre-buffering** - Buffer next song in playlist
4. **Offline Fallback** - Automatically switch to local if available when streaming fails
5. **Stream Caching** - Cache streamed content for offline playback

## Testing Considerations

### Local Playback Tests
- Verify local playback still works
- Check that streaming states don't interfere with local mode

### Streaming Tests
- Test buffering state transitions
- Verify connection loss detection
- Test retry functionality
- Validate state restoration after retry

### Integration Tests
- Test switching between local and streaming
- Verify playlist behavior with mixed sources
- Test seek during streaming

## Notes

- **Stream URI**: Currently uses placeholder URI (`streaming://phone/{songId}`). Replace with actual implementation from StreamingRepository.
- **Model Conversion**: Temporary converter exists between domain and model Song. Consider updating PlaybackManager to use domain entities directly.
- **Error Messages**: Customize error messages based on specific failure scenarios for better UX.
