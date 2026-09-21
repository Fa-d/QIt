package dev.sadakat.qit.wear.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dev.sadakat.qit.wear.service.MusicPlaybackService
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.StreamingStatus
import dev.sadakat.qit.shared.domain.valueobject.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio playback using Media3 ExoPlayer
 * Supports both local and streamed playback with buffering and connection loss handling
 *
 * The ExoPlayer is injected (app-wide singleton, shared with MusicPlaybackService)
 * so that UI controls and the MediaSession always act on the same player.
 */
@Singleton
class PlaybackManager @Inject constructor(
    private val context: Context,
    private val streamingRepository: StreamingRepository,
    private val exoPlayer: ExoPlayer,
    private val coroutineScope: CoroutineScope
) {

    companion object {
        private const val TAG = "PlaybackManager"
    }

    val player: Player get() = exoPlayer

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _streamingPlaybackState = MutableStateFlow<StreamingPlaybackState>(StreamingPlaybackState.Idle)
    val streamingPlaybackState: StateFlow<StreamingPlaybackState> = _streamingPlaybackState.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private var currentPlaybackMode: PlaybackMode = PlaybackMode.Local
    private var streamingMonitorJob: Job? = null
    private var lastKnownPosition: Long = 0

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    _playbackState.value = PlaybackState.Idle
                    if (currentPlaybackMode == PlaybackMode.Streaming) {
                        _streamingPlaybackState.value = StreamingPlaybackState.Idle
                    }
                }
                Player.STATE_BUFFERING -> {
                    _playbackState.value = PlaybackState.Buffering
                    if (currentPlaybackMode == PlaybackMode.Streaming) {
                        val progress = exoPlayer.bufferedPercentage / 100f
                        _streamingPlaybackState.value = StreamingPlaybackState.Buffering(progress)
                    }
                }
                Player.STATE_READY -> {
                    _playbackState.value = PlaybackState.Ready
                    if (currentPlaybackMode == PlaybackMode.Streaming) {
                        _currentSong.value?.let { song ->
                            _streamingPlaybackState.value = StreamingPlaybackState.Ready(song)
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    _playbackState.value = PlaybackState.Ended
                    stopStreamingMonitor()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                lastKnownPosition = exoPlayer.currentPosition
                _currentSong.value?.let { song ->
                    if (currentPlaybackMode == PlaybackMode.Streaming) {
                        _streamingPlaybackState.value = StreamingPlaybackState.Playing(song, exoPlayer.currentPosition)
                    }
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _streamingPlaybackState.value = StreamingPlaybackState.Error(
                error.message ?: "Playback error occurred"
            )
            handlePlaybackError(error)
        }
    }

    // Attach the listener after it is initialized (init blocks and property
    // initializers run in declaration order).
    init {
        exoPlayer.addListener(playerListener)
    }

    /**
     * Ensures the MediaSessionService is running so playback survives the app
     * going to the background (and so a media notification is shown).
     */
    private fun ensurePlaybackServiceStarted() {
        try {
            context.startService(Intent(context, MusicPlaybackService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start MusicPlaybackService", e)
        }
    }

    /**
     * Play a song from local storage
     */
    fun playLocalSong(song: Song) {
        ensurePlaybackServiceStarted()
        stopStreamingMonitor()
        currentPlaybackMode = PlaybackMode.Local

        // Check if song is downloaded on watch
        val downloadStatus = song.downloadStatus
        if (downloadStatus !is DownloadStatus.Downloaded) {
            Log.e(TAG, "Song is not downloaded on watch")
            return
        }

        val localPath = downloadStatus.localPath ?: return
        val file = File(localPath)
        if (!file.exists()) {
            Log.e(TAG, "Local file not found: $localPath")
            return
        }

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setMediaId(song.id.value)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .build()
            )
            .build()

        _currentSong.value = song
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    /**
     * Play a song streamed from phone
     * Sets up streaming source and monitors buffering/connection status
     */
    fun playStreamedSong(song: Song, streamUri: Uri) {
        ensurePlaybackServiceStarted()
        currentPlaybackMode = PlaybackMode.Streaming
        _currentSong.value = song
        _streamingPlaybackState.value = StreamingPlaybackState.Buffering(0f)

        val mediaItem = MediaItem.Builder()
            .setUri(streamUri)
            .setMediaId(song.id.value)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .build()
            )
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()

        // Monitor streaming status
        startStreamingMonitor()
    }

    
    /**
     * Switches between local and streamed playback
     * Preserves playback position when switching
     */
    fun switchPlaybackMode(song: Song, mode: PlaybackMode, streamUri: Uri? = null) {
        val currentPosition = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying

        when (mode) {
            PlaybackMode.Local -> {
                stopStreamingMonitor()
                playLocalSong(song)
                exoPlayer.seekTo(currentPosition)
                if (wasPlaying) exoPlayer.play() else exoPlayer.pause()
            }
            PlaybackMode.Streaming -> {
                if (streamUri != null) {
                    playStreamedSong(song, streamUri)
                    // The streaming buffer is a FIFO: no position-preserving
                    // seek after switching to a stream (see seekTo).
                    if (wasPlaying) exoPlayer.play() else exoPlayer.pause()
                }
            }
        }
    }

    /**
     * Play/pause toggle
     */
    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Play
     */
    fun play() {
        exoPlayer.play()
    }

    /**
     * Pause
     */
    fun pause() {
        exoPlayer.pause()
    }

    /**
     * Stop playback
     */
    fun stop() {
        exoPlayer.stop()
        _currentSong.value = null
        _playbackState.value = PlaybackState.Idle
    }

    /**
     * Seek to position.
     *
     * NOT supported while streaming: the streaming buffer is a FIFO with no
     * byte-index mapping, so a seek would need a protocol extension to
     * re-request from an offset. No-op with a log instead of corrupting
     * playback.
     */
    fun seekTo(positionMs: Long) {
        if (currentPlaybackMode == PlaybackMode.Streaming) {
            Log.w(TAG, "seekTo($positionMs) ignored: seeking is not supported in streaming mode")
            return
        }
        exoPlayer.seekTo(positionMs)
    }

    /**
     * Skip to next song
     */
    fun skipToNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    /**
     * Skip to previous song
     */
    fun skipToPrevious() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    /**
     * Set playlist
     */
    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        ensurePlaybackServiceStarted()
        val mediaItems = songs.mapNotNull { song ->
            val downloadStatus = song.downloadStatus
            if (downloadStatus is DownloadStatus.Downloaded) {
                val localPath = downloadStatus.localPath
                val file = File(localPath)
                if (file.exists()) {
                    MediaItem.Builder()
                        .setUri(Uri.fromFile(file))
                        .setMediaId(song.id.value)
                        .build()
                } else {
                    null
                }
            } else null
        }

        if (mediaItems.isNotEmpty()) {
            exoPlayer.setMediaItems(mediaItems, startIndex, 0)
            exoPlayer.prepare()
            if (startIndex < songs.size) {
                _currentSong.value = songs[startIndex]
            }
        }
    }

    /**
     * Get current playback position
     */
    fun getCurrentPosition(): Long {
        return exoPlayer.currentPosition
    }

    /**
     * Get total duration
     */
    fun getDuration(): Long {
        return exoPlayer.duration
    }

    /**
     * Release resources
     */
    fun release() {
        stopStreamingMonitor()
        exoPlayer.release()
    }

    /**
     * Starts monitoring streaming status
     */
    private fun startStreamingMonitor() {
        streamingMonitorJob?.cancel()
        streamingMonitorJob = streamingRepository.observeStreamingStatus()
            .onEach { status ->
                handleStreamingStatus(status)
            }
            .launchIn(coroutineScope)
    }

    /**
     * Stops monitoring streaming status
     */
    private fun stopStreamingMonitor() {
        streamingMonitorJob?.cancel()
        streamingMonitorJob = null
    }

    /**
     * Handles streaming status updates
     */
    private fun handleStreamingStatus(status: StreamingStatus) {
        when (status) {
            is StreamingStatus.Idle -> {
                // Streaming stopped
            }
            is StreamingStatus.Streaming -> {
                // Successfully streaming
            }
            is StreamingStatus.Buffering -> {
                _streamingPlaybackState.value = StreamingPlaybackState.Buffering(status.progress)
            }
            is StreamingStatus.Error -> {
                _streamingPlaybackState.value = StreamingPlaybackState.Error(status.message)
                handleConnectionLoss()
            }
        }
    }

    /**
     * Handles connection loss during streaming
     * 1. Stop playback immediately (tears down the loader on the aborted buffer)
     * 2. Show "Connection lost" state
     * 3. Store last position for potential resume
     */
    private fun handleConnectionLoss() {
        if (currentPlaybackMode != PlaybackMode.Streaming) return

        // Record the position, then STOP (not pause): the stream buffer has
        // been aborted, so any parked loader read must be torn down and the
        // player must leave the buffering state.
        lastKnownPosition = exoPlayer.currentPosition
        exoPlayer.stop()

        // Update state to show connection lost
        _streamingPlaybackState.value = StreamingPlaybackState.Error("Connection lost")
    }

    /**
     * Attempts to retry streaming after connection loss.
     *
     * The streaming buffer is a FIFO: playback cannot resume at an arbitrary
     * byte offset, so the retry RESTARTS the stream from the beginning
     * (stop old stream -> fresh request from the phone -> position 0)
     * instead of seeking to the last known position.
     */
    suspend fun retryStreaming(streamUri: Uri): Result<Unit> {
        val song = _currentSong.value
            ?: return Result.failure(IllegalStateException("No current song"))
        return try {
            _streamingPlaybackState.value = StreamingPlaybackState.Buffering(0f)

            // Kill the old loader first so it stops reading the old buffer.
            withContext(Dispatchers.Main) { exoPlayer.stop() }

            // Stop the old stream (aborts + removes its buffer, closes its
            // channel) and request a fresh one.
            streamingRepository.stopStreaming(song.id)
            val quality = streamingRepository.getRecommendedQuality()
            val requestResult = streamingRepository.requestStreamFromPhone(song.id, quality)
            if (requestResult.isFailure) {
                val error = requestResult.exceptionOrNull()?.message ?: "Stream request failed"
                _streamingPlaybackState.value = StreamingPlaybackState.Error("Retry failed: $error")
                return requestResult
            }

            val mediaItem = MediaItem.Builder()
                .setUri(streamUri)
                .setMediaId(song.id.value)
                .build()

            // setMediaItem resets the position to 0 - intentional (see kdoc).
            withContext(Dispatchers.Main) {
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            }

            startStreamingMonitor()
            Result.success(Unit)
        } catch (e: Exception) {
            _streamingPlaybackState.value = StreamingPlaybackState.Error(
                "Retry failed: ${e.message}"
            )
            Result.failure(e)
        }
    }

    /**
     * Handles playback errors from ExoPlayer
     */
    private fun handlePlaybackError(error: androidx.media3.common.PlaybackException) {
        when (error.errorCode) {
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                if (currentPlaybackMode == PlaybackMode.Streaming) {
                    handleConnectionLoss()
                }
            }
            else -> {
                // Handle other errors
                _streamingPlaybackState.value = StreamingPlaybackState.Error(
                    error.message ?: "Playback error"
                )
            }
        }
    }

    /**
     * Custom DataSource that delegates based on URI scheme
     * - streaming:// → StreamingAudioSource (reads from buffer)
     * - file:// → FileDataSource (reads from local storage)
     */
}

/**
 * Playback state sealed class
 */
sealed class PlaybackState {
    object Idle : PlaybackState()
    object Buffering : PlaybackState()
    object Ready : PlaybackState()
    object Ended : PlaybackState()
}

/**
 * Streaming-aware playback state
 */
sealed class StreamingPlaybackState {
    object Idle : StreamingPlaybackState()
    data class Buffering(val progress: Float) : StreamingPlaybackState()
    data class Ready(val song: Song) : StreamingPlaybackState()
    data class Playing(val song: Song, val position: Long) : StreamingPlaybackState()
    data class Error(val message: String) : StreamingPlaybackState()
}

/**
 * Playback mode
 */
enum class PlaybackMode {
    Local,
    Streaming
}
