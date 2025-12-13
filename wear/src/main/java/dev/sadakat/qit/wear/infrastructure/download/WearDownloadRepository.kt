package dev.sadakat.qit.wear.infrastructure.download

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.FileSize
import dev.sadakat.qit.wear.data.local.dao.PlaylistDao
import dev.sadakat.qit.wear.data.local.dao.SongDao
import dev.sadakat.qit.wear.data.local.entity.SongEntity
import dev.sadakat.qit.wear.infrastructure.storage.StorageManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watch-side implementation of DownloadRepository
 * Handles receiving downloaded files from phone via Wearable Channel API
 */
@Singleton
class WearDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageClient: MessageClient,
    private val channelClient: ChannelClient,
    private val nodeClient: NodeClient,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val storageManager: StorageManager
) : DownloadRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track download progress for each song
    private val downloadProgressMap = mutableMapOf<SongId, MutableStateFlow<Float>>()

    // Track active downloads
    private val activeDownloads = mutableMapOf<SongId, Job>()

    // Track paused downloads
    private val pausedDownloads = mutableSetOf<SongId>()

    // Track downloading queue
    private val _downloadingQueue = MutableStateFlow<List<Song>>(emptyList())

    // Listener for incoming download channels
    private val channelCallback = object : ChannelClient.ChannelCallback() {
        override fun onChannelOpened(channel: ChannelClient.Channel) {
            Log.d(TAG, "Channel opened: ${channel.path}")

            // Check if this is a download channel
            if (channel.path.startsWith(WearPaths.DOWNLOAD_CHANNEL)) {
                val songId = channel.path.removePrefix(WearPaths.DOWNLOAD_CHANNEL)
                scope.launch {
                    handleIncomingDownload(channel, SongId.from(songId))
                }
            }
        }

        override fun onChannelClosed(
            channel: ChannelClient.Channel,
            closeReason: Int,
            appSpecificErrorCode: Int
        ) {
            Log.d(TAG, "Channel closed: ${channel.path}, reason: $closeReason")

            if (channel.path.startsWith(WearPaths.DOWNLOAD_CHANNEL)) {
                val songId = SongId.from(channel.path.removePrefix(WearPaths.DOWNLOAD_CHANNEL))

                // CLOSE_REASON_NORMAL = 0, CLOSE_REASON_REMOTE_CLOSE = 2
                if (closeReason != 0 && closeReason != 2) {
                    // Download failed
                    scope.launch {
                        Log.e(TAG, "Download failed for song: $songId, reason: $closeReason")
                        activeDownloads.remove(songId)
                        updateDownloadingQueue()
                    }
                }
            }
        }
    }

    init {
        // Register channel callback to listen for incoming downloads
        channelClient.registerChannelCallback(channelCallback)
    }

    override suspend fun downloadSong(
        songId: SongId,
        quality: AudioQuality
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Requesting download from phone: songId=$songId, quality=$quality")

            // Check if already downloaded
            val existingSong = songDao.getSongById(songId.value)
            if (existingSong?.isDownloaded == true) {
                Log.d(TAG, "Song already downloaded: $songId")
                return@withContext Result.success(Unit)
            }

            // Check if already downloading
            if (activeDownloads.containsKey(songId)) {
                Log.d(TAG, "Song already downloading: $songId")
                return@withContext Result.success(Unit)
            }

            // Check available storage
            val availableStorage = storageManager.getAvailableStorage()
            if (availableStorage.bytes < MIN_REQUIRED_STORAGE_BYTES) {
                Log.w(TAG, "Insufficient storage space")
                // Try to cleanup old downloads
                storageManager.cleanupOldDownloads(FileSize.fromMegabytes(50))

                // Check again
                val newAvailable = storageManager.getAvailableStorage()
                if (newAvailable.bytes < MIN_REQUIRED_STORAGE_BYTES) {
                    return@withContext Result.failure(
                        Exception("Insufficient storage space. Available: ${newAvailable.format()}")
                    )
                }
            }

            // Get connected nodes
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return@withContext Result.failure(Exception("No phone connected"))
            }

            // Initialize progress tracking
            downloadProgressMap[songId] = MutableStateFlow(0f)

            // Update song status to downloading
            songDao.updateDownloadProgress(songId.value, 0f)
            updateDownloadingQueue()

            // Send download request to phone
            val requestData = "${songId.value}:${quality.name}".toByteArray()
            val node = nodes.first()
            messageClient.sendMessage(
                node.id,
                WearPaths.DOWNLOAD_REQUEST,
                requestData
            ).await()

            Log.d(TAG, "Download request sent to phone for song: $songId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request download from phone", e)
            downloadProgressMap.remove(songId)
            updateDownloadingQueue()
            Result.failure(e)
        }
    }

    override suspend fun downloadPlaylist(
        playlistId: PlaylistId,
        quality: AudioQuality
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading playlist: $playlistId")

            // Get playlist songs
            val playlist = playlistDao.getPlaylistById(playlistId.value)
                ?: return@withContext Result.failure(Exception("Playlist not found"))

            val songIdStrings = if (playlist.songIds.isEmpty()) emptyList() else playlist.songIds.split(",")
            val songIds = songIdStrings.map { SongId.from(it) }

            // Download each song
            var successCount = 0
            var failureCount = 0

            for (songId in songIds) {
                val result = downloadSong(songId, quality)
                if (result.isSuccess) {
                    successCount++
                } else {
                    failureCount++
                    Log.w(TAG, "Failed to download song: $songId")
                }
            }

            Log.d(TAG, "Playlist download initiated: $successCount succeeded, $failureCount failed")

            if (failureCount > 0 && successCount == 0) {
                Result.failure(Exception("Failed to download any songs from playlist"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download playlist", e)
            Result.failure(e)
        }
    }

    override suspend fun cancelDownload(songId: SongId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Canceling download: $songId")

            // Cancel active job
            activeDownloads[songId]?.cancel()
            activeDownloads.remove(songId)

            // Remove from paused downloads
            pausedDownloads.remove(songId)

            // Clear progress
            downloadProgressMap.remove(songId)

            // Reset download status in database
            songDao.updateDownloadProgress(songId.value, 0f)
            updateDownloadingQueue()

            // Send cancellation message to phone
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isNotEmpty()) {
                    val cancelData = songId.value.toByteArray()
                    messageClient.sendMessage(
                        nodes.first().id,
                        "${WearPaths.DOWNLOAD_REQUEST}/cancel",
                        cancelData
                    ).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send cancellation to phone", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel download", e)
            Result.failure(e)
        }
    }

    override suspend fun pauseDownload(songId: SongId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Pausing download: $songId")

            // Mark as paused
            pausedDownloads.add(songId)

            // Cancel active job (will be resumed later)
            activeDownloads[songId]?.cancel()
            activeDownloads.remove(songId)

            updateDownloadingQueue()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause download", e)
            Result.failure(e)
        }
    }

    override suspend fun resumeDownload(songId: SongId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Resuming download: $songId")

            if (!pausedDownloads.contains(songId)) {
                return@withContext Result.failure(Exception("Download is not paused"))
            }

            pausedDownloads.remove(songId)

            // Request download again (phone will resume if supported)
            downloadSong(songId, AudioQuality.MEDIUM)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume download", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteDownload(songId: SongId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting download: $songId")

            // Cancel if downloading
            if (activeDownloads.containsKey(songId)) {
                cancelDownload(songId)
            }

            // Delete file
            storageManager.deleteSong(songId)

            // Update database
            songDao.updateDownloadStatus(songId.value, false, null)
            songDao.updateDownloadProgress(songId.value, 0f)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete download", e)
            Result.failure(e)
        }
    }

    override fun observeDownloadProgress(songId: SongId): Flow<Float> {
        return downloadProgressMap.getOrPut(songId) {
            MutableStateFlow(0f)
        }.asStateFlow()
    }

    override fun getDownloadingQueue(): Flow<List<Song>> {
        return _downloadingQueue.asStateFlow()
    }

    override suspend fun getAvailableStorage(): FileSize {
        return storageManager.getAvailableStorage()
    }

    override suspend fun getTotalDownloadedSize(): FileSize {
        return storageManager.getTotalDownloadedSize()
    }

    override suspend fun cleanupOldDownloads(targetFreeSpace: FileSize): Result<Int> {
        return try {
            val deletedCount = storageManager.cleanupOldDownloads(targetFreeSpace)
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old downloads", e)
            Result.failure(e)
        }
    }

    /**
     * Handle incoming download from phone via channel
     */
    private suspend fun handleIncomingDownload(
        channel: ChannelClient.Channel,
        songId: SongId
    ) {
        val downloadJob = scope.launch {
            try {
                Log.d(TAG, "Handling incoming download for song: $songId")

                // Check if paused
                if (pausedDownloads.contains(songId)) {
                    Log.d(TAG, "Download is paused, closing channel: $songId")
                    channelClient.close(channel).await()
                    return@launch
                }

                // Get input stream from channel
                val inputStream = channelClient.getInputStream(channel).await()

                // Create download directory if not exists
                val downloadDir = File(context.filesDir, DOWNLOAD_DIR)
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                // Create file for downloaded song
                val file = File(downloadDir, "${songId.value}.mp3")

                // Read from channel and write to file
                val buffer = ByteArray(8192)
                var totalBytesRead = 0L
                var lastProgressUpdate = 0L

                FileOutputStream(file).use { output ->
                    inputStream.use { input ->
                        while (isActive && !pausedDownloads.contains(songId)) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) {
                                Log.d(TAG, "Reached end of download stream")
                                break
                            }

                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            // Update progress (estimate based on average song size)
                            // We'll get more accurate progress via message updates
                            val progress = (totalBytesRead.toFloat() / ESTIMATED_SONG_SIZE_BYTES)
                                .coerceIn(0f, 0.99f)

                            // Only update every 100KB to avoid too many updates
                            if (totalBytesRead - lastProgressUpdate > 100_000) {
                                downloadProgressMap[songId]?.value = progress
                                songDao.updateDownloadProgress(songId.value, progress)
                                lastProgressUpdate = totalBytesRead
                                Log.d(TAG, "Download progress: ${(progress * 100).toInt()}% ($totalBytesRead bytes)")
                            }
                        }
                    }
                }

                // Check if download was completed (not paused or cancelled)
                if (isActive && !pausedDownloads.contains(songId)) {
                    Log.d(TAG, "Download completed: $songId, size: $totalBytesRead bytes")

                    // Update database - mark as downloaded
                    songDao.updateDownloadStatus(songId.value, true, file.absolutePath)
                    songDao.updateDownloadProgress(songId.value, 1f)

                    // Update progress to 100%
                    downloadProgressMap[songId]?.value = 1f

                    // Remove from active downloads
                    activeDownloads.remove(songId)
                    downloadProgressMap.remove(songId)
                    updateDownloadingQueue()

                    Log.d(TAG, "Song successfully downloaded to: ${file.absolutePath}")
                } else {
                    // Download was cancelled or paused, delete partial file
                    Log.d(TAG, "Download cancelled or paused, deleting partial file")
                    file.delete()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming download", e)

                // Clean up on error
                activeDownloads.remove(songId)
                downloadProgressMap.remove(songId)
                updateDownloadingQueue()

                // Delete partial file
                val downloadDir = File(context.filesDir, DOWNLOAD_DIR)
                val file = File(downloadDir, "${songId.value}.mp3")
                if (file.exists()) {
                    file.delete()
                }
            }
        }

        activeDownloads[songId] = downloadJob
        updateDownloadingQueue()
    }

    /**
     * Update the downloading queue state
     */
    private suspend fun updateDownloadingQueue() {
        try {
            val downloadingSongIds = activeDownloads.keys.map { it.value }
            if (downloadingSongIds.isEmpty()) {
                _downloadingQueue.value = emptyList()
                return
            }

            val songs = songDao.getSongsByIds(downloadingSongIds)
                .map { it.toDomainSong() }

            _downloadingQueue.value = songs
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update downloading queue", e)
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        channelClient.unregisterChannelCallback(channelCallback)
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
        downloadProgressMap.clear()
        scope.cancel()
    }

    companion object {
        private const val TAG = "WearDownloadRepo"
        private const val DOWNLOAD_DIR = "downloads"
        private const val MIN_REQUIRED_STORAGE_BYTES = 10 * 1024 * 1024L // 10 MB
        private const val ESTIMATED_SONG_SIZE_BYTES = 5 * 1024 * 1024L // 5 MB average
    }
}
