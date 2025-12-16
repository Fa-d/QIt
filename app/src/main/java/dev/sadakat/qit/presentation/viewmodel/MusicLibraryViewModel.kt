package dev.sadakat.qit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.application.usecase.music.GetAllSongsUseCase
import dev.sadakat.qit.application.usecase.music.ScanMusicLibraryUseCase
import dev.sadakat.qit.playback.PlaybackManager
import dev.sadakat.qit.shared.domain.entity.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Music Library screen
 * Uses use cases following Clean Architecture principles
 */
@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val scanMusicLibraryUseCase: ScanMusicLibraryUseCase,
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()

    init {
        loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            getAllSongsUseCase().collect { songs ->
                _songs.value = songs
            }
        }
    }

    fun scanMusicLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            val result = scanMusicLibraryUseCase()
            _isScanning.value = false

            _scanResult.value = if (result.isSuccess) {
                ScanResult.Success(result.getOrNull()?.size ?: 0)
            } else {
                ScanResult.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun clearScanResult() {
        _scanResult.value = null
    }

    /**
     * Play a song from the library
     */
    fun playSong(song: Song) {
        playbackManager.playSong(song)
    }

    sealed class ScanResult {
        data class Success(val songsFound: Int) : ScanResult()
        data class Error(val message: String) : ScanResult()
    }
}
