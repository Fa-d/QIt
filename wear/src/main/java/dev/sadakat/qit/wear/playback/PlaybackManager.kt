package dev.sadakat.qit.wear.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.StreamingStatus
import dev.sadakat.qit.shared.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio playback using Media3 ExoPlayer
 * Supports both local and streamed playback with buffering and connection loss handling
 */
@Singleton
class PlaybackManager @Inject constructor(
    private val context: Context,
    private val streamingRepository: StreamingRepository,
    private val coroutineScope: CoroutineScope
) {

    private val _player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            addListener(playerListener)
        }
    }

    val player: Player get() = _player

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
                        val progress = _player.bufferedPercentage / 100f
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
                lastKnownPosition = _player.currentPosition
                _currentSong.value?.let { song ->
                    if (currentPlaybackMode == PlaybackMode.Streaming) {
                        _streamingPlaybackState.value = StreamingPlaybackState.Playing(song, _player.currentPosition)
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

    /**
     * Play a song from local storage
     */
    fun playLocalSong(song: Song) {
        stopStreamingMonitor()
        currentPlaybackMode = PlaybackMode.Local

        val filePath = song.watchFilePath ?: return
        val file = File(filePath)
        if (!file.exists()) return

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setMediaId(song.id)
            .build()

        _currentSong.value = song
        _player.setMediaItem(mediaItem)
        _player.prepare()
        _player.play()
    }

    /**
     * Play a song streamed from phone
     * Sets up streaming source and monitors buffering/connection status
     */
    fun playStreamedSong(song: Song, streamUri: Uri) {
        currentPlaybackMode = PlaybackMode.Streaming
        _currentSong.value = song
        _streamingPlaybackState.value = StreamingPlaybackState.Buffering(0f)

        val mediaItem = MediaItem.Builder()
            .setUri(streamUri)
            .setMediaId(song.id)
            .build()

        _player.setMediaItem(mediaItem)
        _player.prepare()
        _player.play()

        // Monitor streaming status
        startStreamingMonitor()
    }

    /**
     * Switches between local and streamed playback
     * Preserves playback position when switching
     */
    fun switchPlaybackMode(song: Song, mode: PlaybackMode, streamUri: Uri? = null) {
        val currentPosition = _player.currentPosition
        val wasPlaying = _player.isPlaying

        when (mode) {
            PlaybackMode.Local -> {
                stopStreamingMonitor()
                playLocalSong(song)
                _player.seekTo(currentPosition)
                if (wasPlaying) _player.play() else _player.pause()
            }
            PlaybackMode.Streaming -> {
                if (streamUri != null) {
                    playStreamedSong(song, streamUri)
                    _player.seekTo(currentPosition)
                    if (wasPlaying) _player.play() else _player.pause()
                }
            }
        }
    }

    /**
     * Play/pause toggle
     */
    fun togglePlayPause() {
        if (_player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Play
     */
    fun play() {
        _player.play()
    }

    /**
     * Pause
     */
    fun pause() {
        _player.pause()
    }

    /**
     * Stop playback
     */
    fun stop() {
        _player.stop()
        _currentSong.value = null
        _playbackState.value = PlaybackState.Idle
    }

    /**
     * Seek to position
     */
    fun seekTo(positionMs: Long) {
        _player.seekTo(positionMs)
    }

    /**
     * Skip to next song
     */
    fun skipToNext() {
        if (_player.hasNextMediaItem()) {
            _player.seekToNextMediaItem()
        }
    }

    /**
     * Skip to previous song
     */
    fun skipToPrevious() {
        if (_player.hasPreviousMediaItem()) {
            _player.seekToPreviousMediaItem()
        }
    }

    /**
     * Set playlist
     */
    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        val mediaItems = songs.mapNotNull { song ->
            song.watchFilePath?.let { filePath ->
                val file = File(filePath)
                if (file.exists()) {
                    MediaItem.Builder()
                        .setUri(Uri.fromFile(file))
                        .setMediaId(song.id)
                        .build()
                } else {
                    null
                }
            }
        }

        if (mediaItems.isNotEmpty()) {
            _player.setMediaItems(mediaItems, startIndex, 0)
            _player.prepare()
            if (startIndex < songs.size) {
                _currentSong.value = songs[startIndex]
            }
        }
    }

    /**
     * Get current playback position
     */
    fun getCurrentPosition(): Long {
        return _player.currentPosition
    }

    /**
     * Get total duration
     */
    fun getDuration(): Long {
        return _player.duration
    }

    /**
     * Release resources
     */
    fun release() {
        stopStreamingMonitor()
        _player.release()
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
     * 1. Pause playback immediately
     * 2. Show "Connection lost" state
     * 3. Store last position for potential resume
     */
    private fun handleConnectionLoss() {
        if (currentPlaybackMode != PlaybackMode.Streaming) return

        // Pause playback immediately
        lastKnownPosition = _player.currentPosition
        pause()

        // Update state to show connection lost
        _streamingPlaybackState.value = StreamingPlaybackState.Error("Connection lost")
    }

    /**
     * Attempts to retry streaming after connection loss
     * Resumes from last known position if successful
     */
    suspend fun retryStreaming(streamUri: Uri): Result<Unit> {
        return try {
            _currentSong.value?.let { song ->
                _streamingPlaybackState.value = StreamingPlaybackState.Buffering(0f)

                val mediaItem = MediaItem.Builder()
                    .setUri(streamUri)
                    .setMediaId(song.id)
                    .build()

                _player.setMediaItem(mediaItem)
                _player.prepare()
                _player.seekTo(lastKnownPosition)
                _player.play()

                startStreamingMonitor()
                Result.success(Unit)
            } ?: Result.failure(IllegalStateException("No current song"))
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
