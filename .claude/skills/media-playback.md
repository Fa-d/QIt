# QIt Media Playback

## Overview

QIt uses Media3 (ExoPlayer) for audio playback on both phone and watch.

## Key Components

### ExoPlayer Setup

```kotlin
val player = ExoPlayer.Builder(context)
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build(),
        /* handleAudioFocus= */ true
    )
    .build()
```

### MediaSession Integration

```kotlin
val mediaSession = MediaSession.Builder(context, player)
    .setSessionActivity(pendingIntent)
    .setCallback(MediaSessionCallback())
    .build()
```

## Wear Playback Service

**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/service/MusicPlaybackService.kt`

```kotlin
class MusicPlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()

        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(object : MediaSession.Callback {
                // Handle media button events
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player?.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
```

**Manifest registration:**
```xml
<service
    android:name=".service.MusicPlaybackService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

## Playback Sources

### 1. Local Playback (Downloaded)

```kotlin
fun playLocalSong(song: Song) {
    val downloadPath = (song.downloadStatus as? DownloadStatus.Downloaded)?.localPath
        ?: return

    val mediaItem = MediaItem.Builder()
        .setUri(Uri.fromFile(File(downloadPath)))
        .setMediaId(song.id.value)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .build()
        )
        .build()

    player.setMediaItem(mediaItem)
    player.prepare()
    player.play()
}
```

### 2. Streaming Playback

Uses custom `StreamingAudioSource` that reads from `StreamingAudioBuffer`:

```kotlin
@OptIn(UnstableApi::class)
fun playStreamingSong(songId: SongId, audioBuffer: StreamingAudioBuffer) {
    val dataSourceFactory = StreamingAudioSource.Factory(audioBuffer)

    player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()

    val mediaItem = MediaItem.Builder()
        .setUri(Uri.parse("streaming://song/${songId.value}"))
        .setMediaId(songId.value)
        .build()

    player.setMediaItem(mediaItem)
    player.prepare()
    player.playWhenReady = true
}
```

## Streaming Audio Components

### StreamingAudioBuffer
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/streaming/StreamingAudioBuffer.kt`

Circular buffer for streaming audio data:

```kotlin
class StreamingAudioBuffer(
    private val capacity: Int = DEFAULT_CAPACITY
) {
    private val buffer = ByteArray(capacity)
    private var writePosition = 0
    private var readPosition = 0
    private var availableBytes = 0

    suspend fun write(data: ByteArray, offset: Int, length: Int): Int
    suspend fun read(buffer: ByteArray, offset: Int, length: Int): Int
    suspend fun availableBytes(): Int
    fun markStreamComplete()
    fun reset()
}
```

### StreamingAudioSource
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/streaming/StreamingAudioSource.kt`

ExoPlayer DataSource that reads from buffer:

```kotlin
@OptIn(UnstableApi::class)
class StreamingAudioSource(
    private val audioBuffer: StreamingAudioBuffer,
    private val minimumBufferBytes: Int = MIN_BUFFER_BYTES
) : BaseDataSource(/* isNetwork = */ true) {

    override fun open(dataSpec: DataSpec): Long {
        // Initialize for streaming
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        // Read from audioBuffer
        return runBlocking {
            audioBuffer.read(buffer, offset, length)
        }
    }

    override fun close() {
        // Cleanup
    }

    class Factory(private val audioBuffer: StreamingAudioBuffer) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return StreamingAudioSource(audioBuffer)
        }
    }
}
```

## Playback Controller

**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/playback/PlaybackController.kt`

```kotlin
class PlaybackController @Inject constructor(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val streamingRepository: StreamingRepository
) {
    private var player: ExoPlayer? = null

    fun play(song: Song) {
        when {
            song.isAvailableOnWatch() -> playLocal(song)
            else -> playStreaming(song)
        }
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    fun release() {
        player?.release()
        player = null
    }
}
```

## Streaming Strategy Selection

**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/StreamingCoordinator.kt`

```kotlin
sealed class StreamingStrategy {
    data class Local(val path: String) : StreamingStrategy()
    data class RealTime(val quality: AudioQuality) : StreamingStrategy()
    data class Progressive(val quality: AudioQuality) : StreamingStrategy()
    data class Unavailable(val reason: String) : StreamingStrategy()
}

class StreamingCoordinator(...) {
    suspend fun determineStreamingStrategy(song: Song): StreamingStrategy {
        // Check if downloaded locally
        if (song.isAvailableOnWatch()) {
            val path = (song.downloadStatus as DownloadStatus.Downloaded).localPath
            return StreamingStrategy.Local(path)
        }

        // Check if phone is connected
        if (!syncRepository.isWatchConnected()) {
            return StreamingStrategy.Unavailable("Phone not connected")
        }

        // Get streaming mode preference
        val mode = settingsRepository.observeStreamingMode().first()
        val quality = streamingRepository.getRecommendedQuality()

        return when (mode) {
            StreamingMode.LOCAL_ONLY -> StreamingStrategy.Unavailable("Local only mode")
            StreamingMode.STREAM_ONLY -> StreamingStrategy.RealTime(quality)
            StreamingMode.PREFER_LOCAL -> StreamingStrategy.RealTime(quality)
            StreamingMode.ADAPTIVE -> StreamingStrategy.Progressive(quality)
        }
    }
}
```

## Audio Quality

**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/AudioQuality.kt`

```kotlin
enum class AudioQuality(val bitrate: Int, val displayName: String) {
    LOW(64, "Low (64 kbps)"),
    MEDIUM(128, "Medium (128 kbps)"),
    HIGH(256, "High (256 kbps)"),
    ORIGINAL(0, "Original Quality");

    fun estimateFileSize(duration: Duration): FileSize {
        if (bitrate == 0) return FileSize.fromBytes(0) // Unknown for original
        val bytes = (bitrate * 1000L / 8) * duration.seconds
        return FileSize.fromBytes(bytes)
    }
}
```

## Playback State

**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/PlaybackState.kt`

```kotlin
sealed class PlaybackState {
    object Idle : PlaybackState()
    data class Playing(val songId: SongId, val position: Long) : PlaybackState()
    data class Paused(val songId: SongId, val position: Long) : PlaybackState()
    object Buffering : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}
```

## Permissions

### Phone (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

### Watch (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

## Key Files

| Component | Path |
|-----------|------|
| MusicPlaybackService | `/wear/src/main/java/dev/sadakat/qit/wear/service/MusicPlaybackService.kt` |
| PlaybackController | `/wear/src/main/java/dev/sadakat/qit/wear/playback/PlaybackController.kt` |
| StreamingAudioBuffer | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/streaming/StreamingAudioBuffer.kt` |
| StreamingAudioSource | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/streaming/StreamingAudioSource.kt` |
| WearStreamingRepository | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/streaming/WearStreamingRepository.kt` |
| StreamingCoordinator | `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/StreamingCoordinator.kt` |
| PlaybackViewModel | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/viewmodel/PlaybackViewModel.kt` |
| PlaybackScreen | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/screens/PlaybackScreen.kt` |
