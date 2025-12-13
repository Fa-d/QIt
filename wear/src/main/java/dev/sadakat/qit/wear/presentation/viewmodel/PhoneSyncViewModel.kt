package dev.sadakat.qit.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.wear.application.usecase.download.DownloadSongUseCase
import dev.sadakat.qit.wear.application.usecase.sync.RequestPlaylistSyncUseCase
import dev.sadakat.qit.wear.presentation.model.SyncStatus
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

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _autoSyncEnabled = MutableStateFlow(true)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

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
                val timestamp = System.currentTimeMillis()
                _lastSyncTime.value = timestamp
                // TODO: Get actual item count from sync result
                SyncStatus.Success(timestamp, itemCount = 0)
            } else {
                SyncStatus.Error(result.exceptionOrNull()?.message ?: "Sync failed")
            }
        }
    }

    /**
     * Trigger manual sync operation
     */
    fun triggerManualSync() {
        requestPlaylistSync()
    }

    /**
     * Observe sync progress updates
     * This can be enhanced to listen to domain events for real-time progress
     */
    fun observeSyncProgress() {
        // TODO: Implement when sync progress events are available
        // This would subscribe to domain events to track sync progress
    }

    /**
     * Toggle auto-sync setting
     */
    fun toggleAutoSync(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        // TODO: Persist this setting to preferences/datastore
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

    sealed class DownloadStatus {
        object Idle : DownloadStatus()
        data class Downloading(val songId: String, val progress: Float) : DownloadStatus()
        data class Success(val songId: String) : DownloadStatus()
        data class Error(val songId: String, val message: String) : DownloadStatus()
    }
}
