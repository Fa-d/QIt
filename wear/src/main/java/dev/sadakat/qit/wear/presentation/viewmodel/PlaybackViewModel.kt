package dev.sadakat.qit.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.DownloadStatus
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
    val streamingPlaybackState = playbackManager.streamingPlaybackState

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _bufferingProgress = MutableStateFlow(0f)
    val bufferingProgress: StateFlow<Float> = _bufferingProgress.asStateFlow()

    private var currentStreamUri: android.net.Uri? = null

    init {
        // Monitor streaming state changes
        viewModelScope.launch {
            streamingPlaybackState.collect { state ->
                when (state) {
                    is dev.sadakat.qit.wear.playback.StreamingPlaybackState.Buffering -> {
                        _isBuffering.value = true
                        _bufferingProgress.value = state.progress
                        _errorMessage.value = null
                    }
                    is dev.sadakat.qit.wear.playback.StreamingPlaybackState.Ready,
                    is dev.sadakat.qit.wear.playback.StreamingPlaybackState.Playing -> {
                        _isBuffering.value = false
                        _bufferingProgress.value = 1f
                        _errorMessage.value = null
                    }
                    is dev.sadakat.qit.wear.playback.StreamingPlaybackState.Error -> {
                        _isBuffering.value = false
                        _errorMessage.value = state.message
                    }
                    is dev.sadakat.qit.wear.playback.StreamingPlaybackState.Idle -> {
                        _isBuffering.value = false
                        _bufferingProgress.value = 0f
                    }
                }
            }
        }
    }

    /**
     * Play a song using clean architecture pattern
     * This method uses the use case to determine playback strategy
     */
    fun playSong(songId: String, playlistId: String? = null) {
        viewModelScope.launch {
            _errorMessage.value = null

            val params = PlaySongUseCase.Params(
                songId = SongId(songId),
                playlistId = playlistId?.let { PlaylistId(it) }
            )

            val result = playSongUseCase(params)

            result.onSuccess { playbackSource ->
                when (playbackSource) {
                    is PlaybackSource.Local -> playLocalSong(playbackSource.song)
                    is PlaybackSource.Streaming -> playStreamedSong(
                        playbackSource.song,
                        playbackSource.strategy
                    )
                }
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to play song"
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
    private fun playStreamedSong(
        song: Song,
        strategy: dev.sadakat.qit.shared.domain.service.StreamingStrategy
    ) {
        // Convert domain Song to model Song for PlaybackManager
        val modelSong = convertToModelSong(song)

        // TODO: Get actual stream URI from streaming repository
        // For now, we create a placeholder URI - this should be replaced with actual implementation
        val streamUri = android.net.Uri.parse("streaming://phone/${song.id.value}")
        currentStreamUri = streamUri

        playbackManager.playStreamedSong(modelSong, streamUri)
    }

    /**
     * Temporary converter until PlaybackManager is updated to use domain entities
     */
    private fun convertToModelSong(domainSong: Song): dev.sadakat.qit.shared.model.Song {
        val watchFilePath = (domainSong.downloadStatus as? DownloadStatus.Downloaded)?.localPath
        return dev.sadakat.qit.shared.model.Song(
            id = domainSong.id.value,
            title = domainSong.title,
            artist = domainSong.artist,
            album = domainSong.album,
            duration = domainSong.duration.milliseconds,
            filePath = domainSong.filePath ?: "",
            uri = domainSong.uri,
            coverArtUri = domainSong.coverArtUri,
            isDownloadedOnWatch = domainSong.isAvailableOnWatch(),
            watchFilePath = watchFilePath,
            fileSize = domainSong.fileSize.bytes,
            mimeType = domainSong.mimeType,
            bitrate = domainSong.bitrate,
            dateAdded = domainSong.dateAdded
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
        val modelSongs = songs.map { convertToModelSong(it) }
        playbackManager.setPlaylist(modelSongs, startIndex)
    }

    fun updatePlaybackPosition() {
        _currentPosition.value = playbackManager.getCurrentPosition()
        _duration.value = playbackManager.getDuration()
    }

    /**
     * Retries streaming after a connection loss
     */
    fun retryStreaming() {
        viewModelScope.launch {
            currentStreamUri?.let { uri ->
                _errorMessage.value = null
                val result = playbackManager.retryStreaming(uri)
                result.onFailure { error ->
                    _errorMessage.value = error.message ?: "Retry failed"
                }
            } ?: run {
                _errorMessage.value = "No stream URI available for retry"
            }
        }
    }

    /**
     * Clears any error messages
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Checks if currently streaming
     */
    fun isStreaming(): Boolean {
        return streamingPlaybackState.value !is dev.sadakat.qit.wear.playback.StreamingPlaybackState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
