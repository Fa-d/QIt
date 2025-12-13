package dev.sadakat.qit.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.wear.application.usecase.playback.PlaySongUseCase
import dev.sadakat.qit.wear.application.usecase.playback.PlaybackSource
import dev.sadakat.qit.wear.playback.PlaybackManager
import dev.sadakat.qit.wear.playback.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playSongUseCase: PlaySongUseCase,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    // Note: currentSong type changed from shared.model.Song to domain.entity.Song
    // This may require updates to UI code that uses this ViewModel
    val currentSong = playbackManager.currentSong
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    /**
     * Play a song using clean architecture pattern
     * This method uses the use case to determine playback strategy
     */
    fun playSong(songId: String, playlistId: String? = null) {
        viewModelScope.launch {
            val params = PlaySongUseCase.Params(
                songId = SongId(songId),
                playlistId = playlistId?.let { PlaylistId(it) }
            )

            val result = playSongUseCase(params)

            result.onSuccess { playbackSource ->
                when (playbackSource) {
                    is PlaybackSource.Local -> playLocalSong(playbackSource.song)
                    is PlaybackSource.Streaming -> playStreamedSong(playbackSource.song)
                }
            }.onFailure { error ->
                // TODO: Handle playback error (e.g., show error state)
            }
        }
    }

    /**
     * Play a song from local storage
     * Note: This is now private - use playSong() instead for proper use case handling
     */
    private fun playLocalSong(song: Song) {
        // Convert domain Song to model Song for PlaybackManager
        // TODO: Update PlaybackManager to use domain entities
        val modelSong = convertToModelSong(song)
        playbackManager.playLocalSong(modelSong)
    }

    /**
     * Play a song streamed from phone
     * Note: This is now private - use playSong() instead for proper use case handling
     */
    private fun playStreamedSong(song: Song) {
        // Convert domain Song to model Song for PlaybackManager
        // TODO: Update PlaybackManager to use domain entities
        val modelSong = convertToModelSong(song)
        playbackManager.playStreamedSong(modelSong)
    }

    /**
     * Temporary converter until PlaybackManager is updated to use domain entities
     */
    private fun convertToModelSong(domainSong: Song): dev.sadakat.qit.shared.model.Song {
        return dev.sadakat.qit.shared.model.Song(
            id = domainSong.id.value,
            title = domainSong.title,
            artist = domainSong.artist,
            album = domainSong.album,
            duration = domainSong.duration,
            filePath = domainSong.filePath ?: "",
            isDownloadedOnWatch = domainSong.isAvailableOnWatch(),
            watchFilePath = domainSong.watchFilePath,
            albumArt = domainSong.albumArt
        )
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
