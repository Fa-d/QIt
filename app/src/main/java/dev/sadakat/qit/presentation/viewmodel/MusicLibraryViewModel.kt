package dev.sadakat.qit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.data.repository.MusicRepository
import dev.sadakat.qit.shared.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository
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
            musicRepository.getAllSongs().collect { songs ->
                _songs.value = songs
            }
        }
    }

    fun scanMusicLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            val result = musicRepository.scanMusicLibrary()
            _isScanning.value = false

            _scanResult.value = if (result.isSuccess) {
                ScanResult.Success(result.getOrDefault(0))
            } else {
                ScanResult.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun clearScanResult() {
        _scanResult.value = null
    }

    sealed class ScanResult {
        data class Success(val songsFound: Int) : ScanResult()
        data class Error(val message: String) : ScanResult()
    }
}
