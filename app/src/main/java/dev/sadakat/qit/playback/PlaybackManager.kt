package dev.sadakat.qit.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.sadakat.qit.service.MusicPlaybackService
import dev.sadakat.qit.shared.domain.valueobject.ShuffleMode
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.Log
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.SettingsRepository
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.valueobject.PlaybackDestination
import dev.sadakat.qit.shared.domain.valueobject.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio playback on the phone using Media3 ExoPlayer.
 * Handles queue management, repeat/shuffle modes, and playback state.
 *
 * The ExoPlayer is injected (app-wide singleton, the same instance that is
 * attached to the MediaSession) so UI controls and the media notification
 * always act on the same player.
 */
@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioFocusManager: AudioFocusManager,
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
    private val exoPlayer: ExoPlayer,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "PlaybackManager"
    }

    // Track the current playback destination
    private val _playbackDestination = MutableStateFlow(PlaybackDestination.PHONE)
    val playbackDestination: StateFlow<PlaybackDestination> = _playbackDestination.asStateFlow()

    init {
        observeAudioFocus()
        observePlaybackDestination()
    }

    val player: Player get() = exoPlayer

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
                val index = exoPlayer.currentMediaItemIndex
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

    // Attach the listener after it is initialized (init blocks and property
    // initializers run in declaration order).
    init {
        exoPlayer.addListener(playerListener)
    }

    /**
     * Prepare and play a single song
     * Checks the default playback destination and either plays locally or streams to watch
     */
    fun playSong(song: Song) {
        coroutineScope.launch {
            try {
                // Check the default playback destination
                val destination = settingsRepository.getPlaybackDestination()

                when (destination) {
                    PlaybackDestination.PHONE -> {
                        // Play on phone
                        Log.d(TAG, "Playing on phone: ${song.title}")
                        val queue = listOf(song)
                        setPlaybackQueue(queue, 0)
                    }
                    PlaybackDestination.WATCH -> {
                        // Stop local playback on phone first
                        Log.d(TAG, "Stopping phone playback, sending to watch: ${song.title}")
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        _isPlaying.value = false
                        _currentSong.value = null

                        // Send playback command to watch
                        Log.d(TAG, "Sending play command to watch: ${song.title}")
                        val result = syncRepository.sendPlaybackCommand("PLAY", song.id)

                        result.fold(
                            onSuccess = {
                                Log.d(TAG, "Successfully sent play command to watch for: ${song.title}")
                                // Update current song even though it's playing on watch
                                _currentSong.value = song
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Failed to send play command to watch: ${error.message}. Falling back to phone playback.")
                                // Fallback to phone playback if watch command fails
                                val queue = listOf(song)
                                setPlaybackQueue(queue, 0)
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error determining playback destination, defaulting to phone", e)
                // Fallback to phone playback
                val queue = listOf(song)
                setPlaybackQueue(queue, 0)
            }
        }
    }

    /**
     * Ensures the MediaSessionService is running so background playback and
     * the media notification work.
     */
    private fun ensurePlaybackServiceStarted() {
        try {
            context.startService(Intent(context, MusicPlaybackService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start MusicPlaybackService", e)
        }
    }

    /**
     * Set the playback queue and optionally start playing at specific index
     */
    fun setPlaybackQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return

        ensurePlaybackServiceStarted()

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

        exoPlayer.setMediaItems(mediaItems, startIndex, 0)
        exoPlayer.prepare()
        exoPlayer.play()
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

        exoPlayer.addMediaItems(mediaItems)
    }

    /**
     * Remove song from queue at specific index
     */
    fun removeFromQueue(index: Int) {
        val queue = _playbackQueue.value.toMutableList()
        if (index !in queue.indices) return

        queue.removeAt(index)
        _playbackQueue.value = queue

        exoPlayer.removeMediaItem(index)

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

        exoPlayer.moveMediaItem(fromIndex, toIndex)

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
        when (_playbackDestination.value) {
            PlaybackDestination.PHONE -> {
                if (exoPlayer.isPlaying) {
                    pause()
                } else {
                    play()
                }
            }
            PlaybackDestination.WATCH -> {
                // Toggle play/pause on watch
                coroutineScope.launch {
                    val command = if (_isPlaying.value) "PAUSE" else "PLAY"
                    syncRepository.sendPlaybackCommand(command)
                    _isPlaying.value = !_isPlaying.value
                }
            }
        }
    }

    /**
     * Resume playback
     */
    fun play() {
        when (_playbackDestination.value) {
            PlaybackDestination.PHONE -> {
                exoPlayer.play()
            }
            PlaybackDestination.WATCH -> {
                // Send play command to watch
                coroutineScope.launch {
                    syncRepository.sendPlaybackCommand("PLAY")
                    _isPlaying.value = true
                }
            }
        }
    }

    /**
     * Pause playback
     */
    fun pause() {
        when (_playbackDestination.value) {
            PlaybackDestination.PHONE -> {
                exoPlayer.pause()
            }
            PlaybackDestination.WATCH -> {
                // Send pause command to watch
                coroutineScope.launch {
                    syncRepository.sendPlaybackCommand("PAUSE")
                    _isPlaying.value = false
                }
            }
        }
    }

    /**
     * Skip to next track
     */
    fun skipToNext() {
        when (_playbackDestination.value) {
            PlaybackDestination.PHONE -> {
                if (exoPlayer.hasNextMediaItem()) {
                    exoPlayer.seekToNextMediaItem()
                } else if (_repeatMode.value == RepeatMode.ALL) {
                    exoPlayer.seekTo(0, 0)
                }
            }
            PlaybackDestination.WATCH -> {
                // Send skip next command to watch
                coroutineScope.launch {
                    syncRepository.sendPlaybackCommand("SKIP_NEXT")
                }
            }
        }
    }

    /**
     * Skip to previous track or restart current if played > 3s
     */
    fun skipToPrevious() {
        when (_playbackDestination.value) {
            PlaybackDestination.PHONE -> {
                if (exoPlayer.currentPosition > 3000) {
                    exoPlayer.seekTo(0)
                } else if (exoPlayer.hasPreviousMediaItem()) {
                    exoPlayer.seekToPreviousMediaItem()
                } else if (_repeatMode.value == RepeatMode.ALL) {
                    exoPlayer.seekTo(exoPlayer.mediaItemCount - 1, 0)
                }
            }
            PlaybackDestination.WATCH -> {
                // Send skip previous command to watch
                coroutineScope.launch {
                    syncRepository.sendPlaybackCommand("SKIP_PREVIOUS")
                }
            }
        }
    }

    /**
     * Seek to specific position
     */
    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    /**
     * Set repeat mode
     */
    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        when (mode) {
            RepeatMode.NONE -> exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
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
        exoPlayer.shuffleModeEnabled = newMode == ShuffleMode.ALL
    }

    /**
     * Clear the playback queue
     */
    fun clearQueue() {
        _playbackQueue.value = emptyList()
        _currentQueueIndex.value = 0
        exoPlayer.clearMediaItems()
        _currentSong.value = null
    }

    /**
     * Get current playback position
     */
    fun getCurrentPosition(): Long = exoPlayer.currentPosition

    /**
     * Get total duration of current track
     */
    fun getDuration(): Long = exoPlayer.duration

    /**
     * Release player resources
     */
    fun release() {
        audioFocusManager.abandonAudioFocus()
        exoPlayer.release()
    }

    /**
     * Get URI for a song based on its storage location.
     * Prefer the MediaStore content URI - raw file paths are unreliable
     * (or inaccessible) under scoped storage on Android 10+.
     */
    private fun getSongUri(song: Song): Uri {
        val contentUri = song.uri?.takeIf { it.isNotBlank() }
        val filePath = song.filePath?.takeIf { it.isNotBlank() }
        return when {
            contentUri != null -> Uri.parse(contentUri)
            filePath != null -> Uri.parse(filePath)
            else -> Uri.EMPTY
        }
    }

    // ---------------------------------------------------------------------
    // Local playback primitives used for remote control from the watch.
    // These ALWAYS act on the local player and ignore the playback
    // destination setting, so a command relayed from the watch can never be
    // bounced back to the watch (which would create a command loop).
    // ---------------------------------------------------------------------

    /**
     * Plays a single song on the phone immediately (ignores playback destination).
     */
    fun playSongOnPhone(song: Song) {
        setPlaybackQueue(listOf(song), 0)
    }

    /** Resumes/starts local playback. */
    fun playLocal() {
        exoPlayer.play()
    }

    /** Pauses local playback. */
    fun pauseLocal() {
        exoPlayer.pause()
    }

    /** Stops local playback and clears the queue. */
    fun stopLocal() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _isPlaying.value = false
        _currentSong.value = null
        _playbackQueue.value = emptyList()
        _currentQueueIndex.value = 0
    }

    /** Skips to the next item in the local queue (if any). */
    fun skipToNextLocal() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        } else if (_repeatMode.value == RepeatMode.ALL && exoPlayer.mediaItemCount > 0) {
            exoPlayer.seekTo(0, 0)
        }
    }

    /** Skips to the previous item in the local queue (if any). */
    fun skipToPreviousLocal() {
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else if (_repeatMode.value == RepeatMode.ALL && exoPlayer.mediaItemCount > 0) {
            exoPlayer.seekTo(exoPlayer.mediaItemCount - 1, 0)
        }
    }

    /**
     * Handle song end based on repeat mode
     */
    private fun handleSongEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                exoPlayer.seekTo(0)
                exoPlayer.play()
            }
            RepeatMode.NONE -> {
                if (exoPlayer.hasNextMediaItem()) {
                    exoPlayer.seekToNextMediaItem()
                } else {
                    // End of queue
                    pause()
                }
            }
            RepeatMode.ALL -> {
                if (exoPlayer.hasNextMediaItem()) {
                    exoPlayer.seekToNextMediaItem()
                } else {
                    exoPlayer.seekTo(0, 0)
                    exoPlayer.play()
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
                exoPlayer.volume = if (isDucked) 0.3f else 1.0f
            }
        }
    }

    /**
     * Observe playback destination changes
     */
    private fun observePlaybackDestination() {
        coroutineScope.launch {
            settingsRepository.observePlaybackDestination().collect { destination ->
                _playbackDestination.value = destination
                Log.d(TAG, "Playback destination changed to: $destination")
            }
        }
    }
}