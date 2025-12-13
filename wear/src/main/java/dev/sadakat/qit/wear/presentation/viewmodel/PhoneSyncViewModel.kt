package dev.sadakat.qit.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.wear.application.usecase.download.DownloadSongUseCase
import dev.sadakat.qit.wear.application.usecase.sync.RequestPlaylistSyncUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneSyncViewModel @Inject constructor(
    private val requestPlaylistSyncUseCase: RequestPlaylistSyncUseCase,
    private val downloadSongUseCase: DownloadSongUseCase
    // TODO: Add CheckPhoneConnectionUseCase when created
    // TODO: Add RequestSongSyncUseCase when created
    // TODO: Add SendPlaybackCommandUseCase when created
) : ViewModel() {

    private val _isPhoneConnected = MutableStateFlow(false)
    val isPhoneConnected: StateFlow<Boolean> = _isPhoneConnected.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    init {
        checkPhoneConnection()
    }

    fun checkPhoneConnection() {
        viewModelScope.launch {
            // TODO: Implement with CheckPhoneConnectionUseCase when available
            // For now, this functionality is temporarily disabled
            // val result = checkPhoneConnectionUseCase()
            // _isPhoneConnected.value = result.getOrDefault(false)
        }
    }

    fun requestPlaylistSync() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            val result = requestPlaylistSyncUseCase()
            _syncStatus.value = if (result.isSuccess) {
                SyncStatus.Success("Playlists synced")
            } else {
                SyncStatus.Error(result.exceptionOrNull()?.message ?: "Sync failed")
            }
        }
    }

    fun requestSongSync() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            // TODO: Implement with RequestSongSyncUseCase when available
            // val result = requestSongSyncUseCase()
            // _syncStatus.value = if (result.isSuccess) {
            //     SyncStatus.Success("Songs synced")
            // } else {
            //     SyncStatus.Error(result.exceptionOrNull()?.message ?: "Sync failed")
            // }
        }
    }

    fun requestSongDownload(songId: String) {
        viewModelScope.launch {
            _downloadStatus.value = DownloadStatus.Downloading(songId, 0f)
            val result = downloadSongUseCase(SongId(songId))
            _downloadStatus.value = if (result.isSuccess) {
                DownloadStatus.Success(songId)
            } else {
                DownloadStatus.Error(songId, result.exceptionOrNull()?.message ?: "Download failed")
            }
        }
    }

    fun sendPlaybackCommand(command: String, songId: String? = null) {
        viewModelScope.launch {
            // TODO: Implement with SendPlaybackCommandUseCase when available
            // sendPlaybackCommandUseCase(SendPlaybackCommandUseCase.Params(command, songId?.let { SongId(it) }))
        }
    }

    fun clearSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    fun clearDownloadStatus() {
        _downloadStatus.value = DownloadStatus.Idle
    }

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        data class Success(val message: String) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }

    sealed class DownloadStatus {
        object Idle : DownloadStatus()
        data class Downloading(val songId: String, val progress: Float) : DownloadStatus()
        data class Success(val songId: String) : DownloadStatus()
        data class Error(val songId: String, val message: String) : DownloadStatus()
    }
}
