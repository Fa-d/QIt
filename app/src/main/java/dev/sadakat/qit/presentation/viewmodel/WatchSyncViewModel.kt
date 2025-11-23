package dev.sadakat.qit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.data.repository.MusicRepository
import dev.sadakat.qit.data.repository.PlaylistRepository
import dev.sadakat.qit.data.repository.WatchSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchSyncViewModel @Inject constructor(
    private val watchSyncRepository: WatchSyncRepository,
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _isWatchConnected = MutableStateFlow(false)
    val isWatchConnected: StateFlow<Boolean> = _isWatchConnected.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        checkWatchConnection()
    }

    fun checkWatchConnection() {
        viewModelScope.launch {
            val isConnected = watchSyncRepository.isWatchConnected()
            _isWatchConnected.value = isConnected
        }
    }

    fun syncPlaylistsToWatch() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            try {
                // Get all playlists
                val playlists = playlistRepository.getAllPlaylists().first()
                val result = watchSyncRepository.syncPlaylistsToWatch(playlists)

                // Also sync all songs metadata
                val songs = musicRepository.getAllSongs().first()
                watchSyncRepository.syncSongsToWatch(songs)

                _syncStatus.value = if (result.isSuccess) {
                    SyncStatus.Success("Playlists synced successfully")
                } else {
                    SyncStatus.Error(result.exceptionOrNull()?.message ?: "Sync failed")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
            }
        }
    }

    fun sendSongToWatch(songId: String) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            try {
                // Get the song details
                val songs = musicRepository.getAllSongs().first()
                val song = songs.find { it.id == songId }
                val filePath = song?.filePath

                if (filePath != null) {
                    val result = watchSyncRepository.sendAudioFileToWatch(songId, filePath)
                    _syncStatus.value = if (result.isSuccess) {
                        SyncStatus.Success("Song sent to watch")
                    } else {
                        SyncStatus.Error(result.exceptionOrNull()?.message ?: "Send failed")
                    }
                } else {
                    _syncStatus.value = SyncStatus.Error("Song file not found")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Send failed")
            }
        }
    }

    fun clearSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        data class Success(val message: String) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }
}
