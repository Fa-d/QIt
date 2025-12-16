package dev.sadakat.qit.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.sadakat.qit.shared.domain.valueobject.ShuffleMode
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio playback on the phone using Media3 ExoPlayer.
 * Handles queue management, repeat/shuffle modes, and playback state.
 */
@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioFocusManager: AudioFocusManager,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "PlaybackManager"
    }

    init {
        observeAudioFocus()
    }

    private val _player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            addListener(playerListener)
        }
    }

    val player: Player get() = _player

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackQueue = MutableStateFlow<List<Song>>(emptyList())
    val playbackQueue: StateFlow<List<Song>> = _playbackQueue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)
    val shuffleMode: StateFlow<ShuffleMode> = _shuffleMode.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                audioFocusManager.requestAudioFocus()
            } else {
                audioFocusManager.abandonAudioFocus()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                val index = _player.currentMediaItemIndex
                _currentQueueIndex.value = index
                // Update current song from queue if available
                val queue = _playbackQueue.value
                if (index in queue.indices) {
                    _currentSong.value = queue[index]
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    // Handle next based on repeat mode
                    handleSongEnded()
                }
                else -> {}
            }
        }
    }

    /**
     * Prepare and play a single song
     */
    fun playSong(song: Song) {
        val queue = listOf(song)
        setPlaybackQueue(queue, 0)
    }

    /**
     * Set the playback queue and optionally start playing at specific index
     */
    fun setPlaybackQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return

        _playbackQueue.value = songs
        _currentQueueIndex.value = startIndex.coerceIn(0, songs.size - 1)

        val mediaItems = songs.map { song ->
            val uri = getSongUri(song)
            MediaItem.Builder()
                .setUri(uri)
                .setMediaId(song.id.value)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(Uri.parse(song.coverArtUri ?: ""))
                        .build()
                )
                .build()
        }

        _player.setMediaItems(mediaItems, startIndex, 0)
        _player.prepare()
        _player.play()
    }

    /**
     * Add songs to the current queue
     */
    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return

        val currentQueue = _playbackQueue.value.toMutableList()
        val currentIndex = _currentQueueIndex.value

        // Add new songs to the queue
        currentQueue.addAll(songs)
        _playbackQueue.value = currentQueue

        // Create media items for new songs
        val mediaItems = songs.map { song ->
            val uri = getSongUri(song)
            MediaItem.Builder()
                .setUri(uri)
                .setMediaId(song.id.value)
                .build()
        }

        _player.addMediaItems(mediaItems)
    }

    /**
     * Remove song from queue at specific index
     */
    fun removeFromQueue(index: Int) {
        val queue = _playbackQueue.value.toMutableList()
        if (index !in queue.indices) return

        queue.removeAt(index)
        _playbackQueue.value = queue

        _player.removeMediaItem(index)

        // Adjust current index if needed
        val currentIndex = _currentQueueIndex.value
        if (index < currentIndex) {
            _currentQueueIndex.value = currentIndex - 1
        } else if (index == currentIndex && index >= queue.size) {
            _currentQueueIndex.value = (queue.size - 1).coerceAtLeast(0)
        }
    }

    /**
     * Move song in queue from one position to another
     */
    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        val queue = _playbackQueue.value.toMutableList()
        if (fromIndex !in queue.indices || toIndex !in queue.indices) return

        val song = queue.removeAt(fromIndex)
        queue.add(toIndex, song)
        _playbackQueue.value = queue

        _player.moveMediaItem(fromIndex, toIndex)

        // Adjust current index if needed
        val currentIndex = _currentQueueIndex.value
        when {
            fromIndex == currentIndex -> _currentQueueIndex.value = toIndex
            fromIndex < currentIndex && toIndex >= currentIndex -> _currentQueueIndex.value = currentIndex - 1
            fromIndex > currentIndex && toIndex <= currentIndex -> _currentQueueIndex.value = currentIndex + 1
        }
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        if (_player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Resume playback
     */
    fun play() {
        _player.play()
    }

    /**
     * Pause playback
     */
    fun pause() {
        _player.pause()
    }

    /**
     * Skip to next track
     */
    fun skipToNext() {
        if (_player.hasNextMediaItem()) {
            _player.seekToNextMediaItem()
        } else if (_repeatMode.value == RepeatMode.ALL) {
            _player.seekTo(0, 0)
        }
    }

    /**
     * Skip to previous track or restart current if played > 3s
     */
    fun skipToPrevious() {
        if (_player.currentPosition > 3000) {
            _player.seekTo(0)
        } else if (_player.hasPreviousMediaItem()) {
            _player.seekToPreviousMediaItem()
        } else if (_repeatMode.value == RepeatMode.ALL) {
            _player.seekTo(_player.mediaItemCount - 1, 0)
        }
    }

    /**
     * Seek to specific position
     */
    fun seekTo(positionMs: Long) {
        _player.seekTo(positionMs)
    }

    /**
     * Set repeat mode
     */
    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        when (mode) {
            RepeatMode.NONE -> _player.repeatMode = Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> _player.repeatMode = Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> _player.repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    /**
     * Toggle shuffle mode
     */
    fun toggleShuffleMode() {
        val newMode = when (_shuffleMode.value) {
            ShuffleMode.OFF -> ShuffleMode.ALL
            ShuffleMode.ALL -> ShuffleMode.OFF
            else -> ShuffleMode.OFF
        }
        _shuffleMode.value = newMode
        _player.shuffleModeEnabled = newMode == ShuffleMode.ALL
    }

    /**
     * Clear the playback queue
     */
    fun clearQueue() {
        _playbackQueue.value = emptyList()
        _currentQueueIndex.value = 0
        _player.clearMediaItems()
        _currentSong.value = null
    }

    /**
     * Get current playback position
     */
    fun getCurrentPosition(): Long = _player.currentPosition

    /**
     * Get total duration of current track
     */
    fun getDuration(): Long = _player.duration

    /**
     * Release player resources
     */
    fun release() {
        audioFocusManager.abandonAudioFocus()
        _player.release()
    }

    /**
     * Get URI for a song based on its storage location
     */
    private fun getSongUri(song: Song): Uri {
        // For now, assume songs are in external storage
        // This can be enhanced based on how songs are stored
        return Uri.parse(song.filePath ?: "")
    }

    /**
     * Handle song end based on repeat mode
     */
    private fun handleSongEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _player.seekTo(0)
                _player.play()
            }
            RepeatMode.NONE -> {
                if (_player.hasNextMediaItem()) {
                    _player.seekToNextMediaItem()
                } else {
                    // End of queue
                    pause()
                }
            }
            RepeatMode.ALL -> {
                if (_player.hasNextMediaItem()) {
                    _player.seekToNextMediaItem()
                } else {
                    _player.seekTo(0, 0)
                    _player.play()
                }
            }
        }
    }

    /**
     * Observe audio focus changes and respond accordingly
     */
    private fun observeAudioFocus() {
        coroutineScope.launch {
            audioFocusManager.hasAudioFocus.collect { hasFocus ->
                if (_isPlaying.value && !hasFocus) {
                    // Lost audio focus while playing, pause playback
                    pause()
                }
            }
        }

        coroutineScope.launch {
            audioFocusManager.isDucked.collect { isDucked ->
                // Adjust volume based on ducking state
                _player.volume = if (isDucked) 0.3f else 1.0f
            }
        }
    }
}