package dev.sadakat.qit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.application.usecase.sync.SyncAllToWatchUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Watch synchronization
 * Uses use cases following Clean Architecture
 */
@HiltViewModel
class WatchSyncViewModel @Inject constructor(
    private val syncAllToWatchUseCase: SyncAllToWatchUseCase
    // TODO: Add CheckWatchConnectionUseCase when created
    // TODO: Add ObserveWatchConnectionUseCase when created (or make CheckWatchConnectionUseCase return a Flow)
) : ViewModel() {

    private val _isWatchConnected = MutableStateFlow(false)
    val isWatchConnected: StateFlow<Boolean> = _isWatchConnected.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        checkWatchConnection()
        observeWatchConnection()
    }

    fun checkWatchConnection() {
        viewModelScope.launch {
            // TODO: Implement with CheckWatchConnectionUseCase when available
            // For now, this functionality is temporarily disabled
            // val result = checkWatchConnectionUseCase()
            // _isWatchConnected.value = result.getOrDefault(false)
        }
    }

    private fun observeWatchConnection() {
        viewModelScope.launch {
            // TODO: Implement with ObserveWatchConnectionUseCase when available
            // This should return a Flow<Boolean> that can be collected
            // observeWatchConnectionUseCase().collect { isConnected ->
            //     _isWatchConnected.value = isConnected
            // }
        }
    }

    fun syncPlaylistsToWatch() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing

            val result = syncAllToWatchUseCase()

            _syncStatus.value = if (result.isSuccess) {
                val syncResult = result.getOrNull()!!
                SyncStatus.Success(
                    "Synced ${syncResult.playlistsSynced} playlists, ${syncResult.songsSynced} songs"
                )
            } else {
                SyncStatus.Error(result.exceptionOrNull()?.message ?: "Sync failed")
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
