package dev.sadakat.qit.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.DownloadStatus
import dev.sadakat.qit.wear.application.usecase.download.CancelDownloadUseCase
import dev.sadakat.qit.wear.application.usecase.download.DownloadSongUseCase
import dev.sadakat.qit.wear.application.usecase.download.GetDownloadedSongsUseCase
import dev.sadakat.qit.wear.application.usecase.storage.GetStorageInfoUseCase
import dev.sadakat.qit.wear.application.usecase.storage.StorageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing downloads on WearOS
 */
@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadSongUseCase: DownloadSongUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val getDownloadedSongsUseCase: GetDownloadedSongsUseCase,
    private val getStorageInfoUseCase: GetStorageInfoUseCase
) : ViewModel() {

    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Map<SongId, Float>>(emptyMap())
    val activeDownloads: StateFlow<Map<SongId, Float>> = _activeDownloads.asStateFlow()

    private val _storageInfo = MutableStateFlow<StorageInfo?>(null)
    val storageInfo: StateFlow<StorageInfo?> = _storageInfo.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadDownloadedSongs()
        loadStorageInfo()
    }

    /**
     * Loads all downloaded songs
     */
    private fun loadDownloadedSongs() {
        viewModelScope.launch {
            getDownloadedSongsUseCase().collect { songs ->
                _downloadedSongs.value = songs
            }
        }
    }

    /**
     * Loads storage information
     */
    fun loadStorageInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            getStorageInfoUseCase()
                .onSuccess { info ->
                    _storageInfo.value = info
                }
                .onFailure { error ->
                    _downloadError.value = error.message ?: "Failed to load storage info"
                }
            _isLoading.value = false
        }
    }

    /**
     * Starts downloading a song
     */
    fun startDownload(songId: SongId) {
        viewModelScope.launch {
            _isLoading.value = true
            _downloadError.value = null

            // Add to active downloads with 0 progress
            _activeDownloads.update { current ->
                current + (songId to 0f)
            }

            downloadSongUseCase(songId)
                .onSuccess {
                    // Download started successfully
                    // Progress will be updated via observeDownloadProgress
                }
                .onFailure { error ->
                    _downloadError.value = error.message ?: "Failed to start download"
                    // Remove from active downloads on failure
                    _activeDownloads.update { current ->
                        current - songId
                    }
                }

            _isLoading.value = false
            // Refresh storage info after download starts
            loadStorageInfo()
        }
    }

    /**
     * Cancels an ongoing download
     */
    fun cancelDownload(songId: SongId) {
        viewModelScope.launch {
            _downloadError.value = null

            cancelDownloadUseCase(songId)
                .onSuccess {
                    // Remove from active downloads
                    _activeDownloads.update { current ->
                        current - songId
                    }
                }
                .onFailure { error ->
                    _downloadError.value = error.message ?: "Failed to cancel download"
                }

            // Refresh storage info
            loadStorageInfo()
        }
    }

    /**
     * Retries a failed download
     */
    fun retryDownload(songId: SongId) {
        startDownload(songId)
    }

    /**
     * Updates download progress for a song
     * This should be called from the infrastructure layer when progress changes
     */
    fun updateDownloadProgress(songId: SongId, progress: Float) {
        _activeDownloads.update { current ->
            current + (songId to progress)
        }

        // If download is complete, remove from active downloads
        if (progress >= 1.0f) {
            viewModelScope.launch {
                _activeDownloads.update { current ->
                    current - songId
                }
                // Refresh storage info
                loadStorageInfo()
            }
        }
    }

    /**
     * Gets download status for a song
     */
    fun getDownloadStatus(song: Song): DownloadStatus {
        // Check if actively downloading
        _activeDownloads.value[song.id]?.let { progress ->
            return DownloadStatus.Downloading(progress)
        }

        // Return the song's current download status
        return song.downloadStatus
    }

    /**
     * Checks if a song is currently downloading
     */
    fun isDownloading(songId: SongId): Boolean {
        return _activeDownloads.value.containsKey(songId)
    }

    /**
     * Gets download progress for a song (0.0 to 1.0)
     */
    fun getDownloadProgress(songId: SongId): Float {
        return _activeDownloads.value[songId] ?: 0f
    }

    /**
     * Clears any download error messages
     */
    fun clearError() {
        _downloadError.value = null
    }

    /**
     * Gets the total number of active downloads
     */
    fun getActiveDownloadCount(): Int {
        return _activeDownloads.value.size
    }

    /**
     * Gets the total number of downloaded songs
     */
    fun getDownloadedSongCount(): Int {
        return _downloadedSongs.value.size
    }
}
