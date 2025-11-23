package dev.sadakat.qit.wear.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.sadakat.qit.shared.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Manages audio playback using Media3 ExoPlayer
 */
class PlaybackManager(private val context: Context) {

    private val _player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            addListener(playerListener)
        }
    }

    val player: Player get() = _player

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> _playbackState.value = PlaybackState.Idle
                Player.STATE_BUFFERING -> _playbackState.value = PlaybackState.Buffering
                Player.STATE_READY -> _playbackState.value = PlaybackState.Ready
                Player.STATE_ENDED -> _playbackState.value = PlaybackState.Ended
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
    }

    /**
     * Play a song from local storage
     */
    fun playLocalSong(song: Song) {
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
     * Note: This requires a custom DataSource implementation
     */
    fun playStreamedSong(song: Song) {
        // TODO: Implement streaming from phone using custom DataSource
        // For now, we'll only support local playback
        _currentSong.value = song
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
        _player.release()
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
