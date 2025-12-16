package dev.sadakat.qit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.application.usecase.connection.GetWatchAppStatusUseCase
import dev.sadakat.qit.application.usecase.connection.ObserveWatchAppStatusUseCase
import dev.sadakat.qit.application.usecase.sync.SyncAllToWatchUseCase
import dev.sadakat.qit.shared.domain.valueobject.WatchAppStatus
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
    private val syncAllToWatchUseCase: SyncAllToWatchUseCase,
    private val getWatchAppStatusUseCase: GetWatchAppStatusUseCase,
    private val observeWatchAppStatusUseCase: ObserveWatchAppStatusUseCase
) : ViewModel() {

    private val _watchAppStatus = MutableStateFlow<WatchAppStatus>(WatchAppStatus.notInstalled())
    val watchAppStatus: StateFlow<WatchAppStatus> = _watchAppStatus.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        refreshWatchStatus()
        observeWatchStatus()
    }

    fun refreshWatchStatus() {
        viewModelScope.launch {
            val result = getWatchAppStatusUseCase()
            result.onSuccess { status ->
                _watchAppStatus.value = status
            }
        }
    }

    private fun observeWatchStatus() {
        viewModelScope.launch {
            observeWatchAppStatusUseCase().collect { status ->
                _watchAppStatus.value = status
            }
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
