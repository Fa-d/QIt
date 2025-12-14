# Watch-Side Streaming Components

This package contains the watch-side implementation for receiving and playing audio streams from the phone.

## Components

### 1. StreamingAudioBuffer
Thread-safe circular buffer for streaming audio data. Handles concurrent writes from the Wearable Channel and reads from ExoPlayer.

**Key Features:**
- Thread-safe read/write operations using Mutex
- Tracks read position and stream completion state
- Provides buffering progress calculation

### 2. StreamingAudioSource
Custom ExoPlayer DataSource that reads from StreamingAudioBuffer. Integrates the streaming buffer with Media3/ExoPlayer for playback.

**Key Features:**
- Implements Media3 DataSource interface
- Reads audio data from buffer on demand
- Handles end-of-stream signaling
- Includes factory for creating instances

### 3. WearStreamingRepository
Watch-side implementation of StreamingRepository. Coordinates receiving audio streams from the phone via Wearable Channel API.

**Key Features:**
- Sends stream requests to phone via MessageClient
- Listens for incoming Channel connections from phone
- Manages audio buffer and streaming lifecycle
- Provides streaming status updates via Flow

## Usage Example

### Requesting a Stream

```kotlin
@Inject
lateinit var streamingRepository: StreamingRepository

// Request stream from phone
val result = streamingRepository.requestStreamFromPhone(
    songId = SongId.from("song-123"),
    quality = AudioQuality.MEDIUM
)

// Observe streaming status
streamingRepository.observeStreamingStatus()
    .collect { status ->
        when (status) {
            is StreamingStatus.Idle -> // Ready to stream
            is StreamingStatus.Buffering -> // Buffering: ${status.progress * 100}%
            is StreamingStatus.Streaming -> // Playing
            is StreamingStatus.Error -> // Error: ${status.message}
        }
    }
```

### Integrating with ExoPlayer

```kotlin
@Inject
lateinit var wearStreamingRepository: WearStreamingRepository

// Get the audio buffer
val audioBuffer = wearStreamingRepository.getAudioBuffer()

// Create DataSource factory
val dataSourceFactory = StreamingAudioSource.Factory(audioBuffer)

// Build ExoPlayer with streaming data source
val player = ExoPlayer.Builder(context)
    .setMediaSourceFactory(
        DefaultMediaSourceFactory(dataSourceFactory)
    )
    .build()

// Create media item for streaming
val mediaItem = MediaItem.Builder()
    .setUri("streaming://song-123") // Dummy URI
    .setMediaId(songId.value)
    .build()

player.setMediaItem(mediaItem)
player.prepare()

// Wait for buffering before starting playback
// (automatically handled by ExoPlayer based on buffer availability)
```

### Stopping a Stream

```kotlin
// Stop current stream
streamingRepository.stopStreaming(songId)
```

## How It Works

1. **Request Phase:**
   - Watch sends stream request message to phone via `WearPaths.AUDIO_STREAM + "request"`
   - Request includes songId and desired quality

2. **Channel Opening:**
   - Phone opens a Channel connection to watch at `WearPaths.AUDIO_STREAM + songId`
   - WearStreamingRepository's ChannelCallback receives the channel

3. **Streaming Phase:**
   - Phone writes audio data to channel InputStream
   - Watch reads from channel and buffers in StreamingAudioBuffer
   - StreamingStatus updates to Buffering with progress

4. **Playback Phase:**
   - Once minimum buffer threshold reached, status updates to Streaming
   - ExoPlayer reads from StreamingAudioSource as needed
   - StreamingAudioSource reads from StreamingAudioBuffer

5. **Completion:**
   - Phone closes channel when done
   - Buffer marked as complete
   - ExoPlayer receives end-of-input signal

## Configuration

### Buffer Size
- **Minimum Buffer**: 64KB (~2 seconds at 256kbps)
- **Target Buffer**: 96KB (~3 seconds at 256kbps)

Configured in:
- `StreamingAudioSource.MIN_BUFFER_BYTES`
- `WearStreamingRepository.TARGET_BUFFER_BYTES`

### Recommended Quality
Default quality for watch streaming is `AudioQuality.MEDIUM` (128kbps) to balance quality and bandwidth.

## Error Handling

The repository handles various error scenarios:
- **No phone connected**: Returns failure when requesting stream
- **Channel close errors**: Updates status to Error state
- **Stream interruption**: Marks buffer as complete and signals end-of-input

## Thread Safety

All components are designed for concurrent access:
- StreamingAudioBuffer uses Mutex for thread-safe operations
- WearStreamingRepository uses coroutines with SupervisorJob
- Channel callbacks handled on IO dispatcher

## Cleanup

To properly clean up resources:

```kotlin
// In ViewModel onCleared() or similar
wearStreamingRepository.cleanup()
```

This will:
- Unregister channel callbacks
- Cancel active streaming jobs
- Release coroutine scope
