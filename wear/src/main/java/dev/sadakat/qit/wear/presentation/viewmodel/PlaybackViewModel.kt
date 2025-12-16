package dev.sadakat.qit.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.DownloadStatus
import dev.sadakat.qit.wear.application.usecase.playback.PlaySongUseCase
import dev.sadakat.qit.wear.application.usecase.playback.PlaybackSource
import dev.sadakat.qit.wear.playback.PlaybackManager
import dev.sadakat.qit.wear.playback.PlaybackState
import dev.sadakat.qit.wear.presentation.model.ConnectionState
import dev.sadakat.qit.wear.presentation.model.StreamingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playSongUseCase: PlaySongUseCase,
    private val playbackManager: PlaybackManager,
    private val streamingRepository: StreamingRepository
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

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _streamingMode = MutableStateFlow<StreamingMode>(StreamingMode.Unknown)
    val streamingMode: StateFlow<StreamingMode> = _streamingMode.asStateFlow()

    private val _volume = MutableStateFlow(0.7f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _phoneBatteryLevel = MutableStateFlow<Float?>(null)
    val phoneBatteryLevel: StateFlow<Float?> = _phoneBatteryLevel.asStateFlow()

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
                        _streamingMode.value = StreamingMode.Streaming
                        _connectionState.value = ConnectionState.Connecting
                    }
                    is dev.sadakat.qit.wear.playback.StreamingPlaybackState.Ready,
                    is dev.sadakat.qit.wear.playback.StreamingPlaybackState.Playing -> {
                        _isBuffering.value = false
                        _bufferingProgress.value = 1f
                        _errorMessage.value = null
                        _connectionState.value = ConnectionState.Connected
                        _streamingMode.value = StreamingMode.Streaming
                    }
                    is dev.sadakat.qit.wear.playback.StreamingPlaybackState.Error -> {
                        _isBuffering.value = false
                        _errorMessage.value = state.message
                        _connectionState.value = ConnectionState.Error(state.message)
                        _streamingMode.value = StreamingMode.Unknown
                    }
                    is dev.sadakat.qit.wear.playback.StreamingPlaybackState.Idle -> {
                        _isBuffering.value = false
                        _bufferingProgress.value = 0f
                        _connectionState.value = ConnectionState.Connected
                    }
                }
            }
        }

        // Monitor current song to update streaming mode
        viewModelScope.launch {
            currentSong.collect { song ->
                song?.let { current ->
                    when (current.downloadStatus) {
                        is DownloadStatus.Downloaded -> {
                            _streamingMode.value = StreamingMode.Offline
                        }
                        else -> {
                            _streamingMode.value = if (isStreaming()) StreamingMode.Streaming else StreamingMode.Unknown
                        }
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
        // PlaybackManager now uses domain entities directly
        playbackManager.playLocalSong(song)
    }

    /**
     * Play a song streamed from phone
     * Note: This is now private - use playSong() instead for proper use case handling
     */
    private fun playStreamedSong(
        song: Song,
        strategy: dev.sadakat.qit.shared.domain.service.StreamingStrategy
    ) {
        viewModelScope.launch {
            when (strategy) {
                is dev.sadakat.qit.shared.domain.service.StreamingStrategy.Local -> {
                    // This shouldn't happen as playStreamedSong is only called for streaming
                    _errorMessage.value = "Invalid strategy for streaming"
                }
                is dev.sadakat.qit.shared.domain.service.StreamingStrategy.RealTime -> {
                    // Request stream from phone
                    val result = streamingRepository.requestStreamFromPhone(
                        songId = song.id,
                        quality = strategy.quality
                    )

                    if (result.isSuccess) {
                        // Create a custom URI that will be handled by a custom DataSource
                        val streamUri = android.net.Uri.parse("streaming://phone/${song.id.value}")
                        currentStreamUri = streamUri

                        // PlaybackManager now uses domain entities directly
                        playbackManager.playStreamedSong(song, streamUri)
                    } else {
                        _errorMessage.value = "Failed to start streaming: ${result.exceptionOrNull()?.message}"
                    }
                }
                is dev.sadakat.qit.shared.domain.service.StreamingStrategy.Progressive -> {
                    // Request stream from phone with progressive download
                    val result = streamingRepository.requestStreamFromPhone(
                        songId = song.id,
                        quality = strategy.quality
                    )

                    if (result.isSuccess) {
                        val streamUri = android.net.Uri.parse("streaming://phone/${song.id.value}")
                        currentStreamUri = streamUri
                        playbackManager.playStreamedSong(song, streamUri)
                    } else {
                        _errorMessage.value = "Failed to start progressive streaming: ${result.exceptionOrNull()?.message}"
                    }
                }
                is dev.sadakat.qit.shared.domain.service.StreamingStrategy.Unavailable -> {
                    _errorMessage.value = "Streaming unavailable: ${strategy.reason}"
                }
            }
        }
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
        // PlaybackManager now uses domain entities directly
        playbackManager.setPlaylist(songs, startIndex)
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

    /**
     * Set volume level
     */
    fun setVolume(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
        // Apply volume to player
        playbackManager.player.volume = _volume.value
    }

    /**
     * Retry connection
     */
    fun retryConnection() {
        retryStreaming()
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
