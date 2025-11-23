package dev.sadakat.qit.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.shared.model.Song
import dev.sadakat.qit.wear.playback.PlaybackManager
import dev.sadakat.qit.wear.playback.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackManager: PlaybackManager
) : ViewModel() {

    val currentSong: StateFlow<Song?> = playbackManager.currentSong
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    fun playLocalSong(song: Song) {
        playbackManager.playLocalSong(song)
    }

    fun playStreamedSong(song: Song) {
        playbackManager.playStreamedSong(song)
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun play() {
        playbackManager.play()
    }

    fun pause() {
        playbackManager.pause()
    }

    fun stop() {
        playbackManager.stop()
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun skipToNext() {
        playbackManager.skipToNext()
    }

    fun skipToPrevious() {
        playbackManager.skipToPrevious()
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        playbackManager.setPlaylist(songs, startIndex)
    }

    fun updatePlaybackPosition() {
        _currentPosition.value = playbackManager.getCurrentPosition()
        _duration.value = playbackManager.getDuration()
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
