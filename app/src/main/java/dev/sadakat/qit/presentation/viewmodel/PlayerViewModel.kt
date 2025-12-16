package dev.sadakat.qit.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.playback.PlaybackManager
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.valueobject.RepeatMode
import dev.sadakat.qit.shared.domain.valueobject.ShuffleMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the music player screen.
 * Manages player state and provides UI-friendly data streams.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackManager: PlaybackManager
) : ViewModel() {

    // Current song
    val currentSong: StateFlow<Song?> = playbackManager.currentSong

    // Playback state
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying

    // Queue state
    val playbackQueue: StateFlow<List<Song>> = playbackManager.playbackQueue
    val currentQueueIndex: StateFlow<Int> = playbackManager.currentQueueIndex

    // Repeat and shuffle modes
    val repeatMode: StateFlow<RepeatMode> = playbackManager.repeatMode
    val shuffleMode: StateFlow<ShuffleMode> = playbackManager.shuffleMode

    // Position tracking (exposed as State for Compose observation)
    private val _currentPosition = mutableStateOf(0L)
    val currentPosition: androidx.compose.runtime.State<Long> = _currentPosition

    private val _duration = mutableStateOf(0L)
    val duration: androidx.compose.runtime.State<Long> = _duration

    // Progress percentage (exposed as State for Compose observation)
    private val _progress = mutableStateOf(0f)
    val progress: androidx.compose.runtime.State<Float> = _progress

    // Show queue state
    var showQueue by mutableStateOf(false)
        private set

    // Position update job
    private var positionUpdateJob: Job? = null

    init {
        startPositionUpdates()
        watchDurationChanges()
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    /**
     * Skip to next track
     */
    fun skipToNext() {
        playbackManager.skipToNext()
    }

    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        playbackManager.skipToPrevious()
    }

    /**
     * Seek to specific position
     */
    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    /**
     * Toggle shuffle mode
     */
    fun toggleShuffle() {
        playbackManager.toggleShuffleMode()
    }

    /**
     * Cycle through repeat modes
     */
    fun toggleRepeatMode() {
        val current = playbackManager.repeatMode.value
        val next = when (current) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        playbackManager.setRepeatMode(next)
    }

    /**
     * Show/hide queue
     */
    fun toggleQueueVisibility() {
        showQueue = !showQueue
    }

    /**
     * Play song at specific queue index
     */
    fun playQueueItem(index: Int) {
        val queue = playbackQueue.value
        if (index in queue.indices) {
            playbackManager.setPlaybackQueue(queue, index)
        }
    }

    /**
     * Remove item from queue
     */
    fun removeFromQueue(index: Int) {
        playbackManager.removeFromQueue(index)
    }

    /**
     * Move item in queue
     */
    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        playbackManager.moveInQueue(fromIndex, toIndex)
    }

    /**
     * Clear the entire queue
     */
    fun clearQueue() {
        playbackManager.clearQueue()
        showQueue = false
    }

    /**
     * Add songs to queue
     */
    fun addToQueue(songs: List<Song>) {
        playbackManager.addToQueue(songs)
    }

    /**
     * Start position tracking updates
     */
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                if (playbackManager.isPlaying.value) {
                    _currentPosition.value = playbackManager.getCurrentPosition()
                    updateProgress()
                }
                delay(1000) // Update every second
            }
        }
    }

    /**
     * Update progress percentage
     */
    private fun updateProgress() {
        _progress.value = if (_duration.value > 0) {
            _currentPosition.value.toFloat() / _duration.value
        } else 0f
    }

    /**
     * Watch for duration changes
     */
    private fun watchDurationChanges() {
        viewModelScope.launch {
            playbackManager.isPlaying.collect { isPlaying ->
                if (isPlaying) {
                    updateDuration()
                }
            }
        }

        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    updateDuration()
                }
            }
        }
    }

    /**
     * Update duration from player
     */
    private fun updateDuration() {
        val playerDuration = playbackManager.getDuration()
        if (playerDuration > 0) {
            _duration.value = playerDuration
            updateProgress()
        }
    }

    /**
     * Manually update playback position
     */
    fun updatePlaybackPosition() {
        if (playbackManager.isPlaying.value) {
            _currentPosition.value = playbackManager.getCurrentPosition()
        }
    }

    /**
     * Get formatted time string
     */
    fun formatTime(milliseconds: Long): String {
        if (milliseconds <= 0) return "0:00"
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
    }
}