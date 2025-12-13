package dev.sadakat.qit.infrastructure.wearable

import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.FileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

/**
 * WearableDownloadRepository implementation (Phone side)
 * Handles downloading audio files from phone to watch via ChannelClient
 */
class WearableDownloadRepository @Inject constructor(
    private val channelClient: ChannelClient,
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : DownloadRepository {

    companion object {
        private const val TAG = "WearableDownloadRepo"
        private const val CHUNK_SIZE = 8192 // 8KB chunks
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    // Track download progress for each song
    private val downloadProgressMap = mutableMapOf<SongId, MutableStateFlow<Float>>()

    // Track active channels for downloads
    private val activeChannels = mutableMapOf<SongId, ChannelClient.Channel>()

    // Track downloading queue
    private val _downloadingQueue = MutableStateFlow<List<Song>>(emptyList())

    // Track paused downloads
    private val pausedDownloads = mutableSetOf<SongId>()

    // Track cancelled downloads
    private val cancelledDownloads = mutableSetOf<SongId>()

    override suspend fun downloadSong(
        songId: SongId,
        quality: AudioQuality
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Starting download for song: ${songId.value} with quality: $quality")

            // Get song details from repository
            val songResult = musicRepository.getSongById(songId)
            if (songResult.isFailure) {
                val error = "Failed to get song details: ${songResult.exceptionOrNull()?.message}"
                Log.e(TAG, error)
                return Result.failure(songResult.exceptionOrNull() ?: Exception(error))
            }

            val song = songResult.getOrNull()
            if (song == null) {
                val error = "Song not found: ${songId.value}"
                Log.e(TAG, error)
                return Result.failure(IllegalStateException(error))
            }

            // Validate file path
            val filePath = song.filePath
            if (filePath.isNullOrBlank()) {
                val error = "Song file path is null or empty"
                Log.e(TAG, error)
                return Result.failure(IllegalStateException(error))
            }

            val file = File(filePath)
            if (!file.exists()) {
                val error = "Song file does not exist: $filePath"
                Log.e(TAG, error)
                return Result.failure(IllegalStateException(error))
            }

            Log.d(TAG, "Song file found: $filePath (${file.length()} bytes)")

            // Check for connected nodes
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                val error = "No watch connected"
                Log.e(TAG, error)
                return Result.failure(IllegalStateException(error))
            }

            val node = nodes.first()
            Log.d(TAG, "Downloading to node: ${node.displayName} (${node.id})")

            // Initialize progress tracking
            mutex.withLock {
                downloadProgressMap[songId] = MutableStateFlow(0f)
                _downloadingQueue.value = _downloadingQueue.value + song
                cancelledDownloads.remove(songId)
                pausedDownloads.remove(songId)
            }

            // Send download start notification to watch
            sendDownloadStartMessage(node.id, songId, quality, file.length())

            // Open channel for download
            val channelPath = "${WearPaths.DOWNLOAD_CHANNEL}${songId.value}"
            Log.d(TAG, "Opening channel: $channelPath")
            val channel = channelClient.openChannel(node.id, channelPath).await()

            mutex.withLock {
                activeChannels[songId] = channel
            }

            // Download the file
            val downloadResult = downloadFile(channel, file, songId)
            if (downloadResult.isFailure) {
                Log.e(TAG, "File download failed", downloadResult.exceptionOrNull())
                cleanupDownload(songId, song)
                sendDownloadCompleteMessage(node.id, songId, false)
                return downloadResult
            }

            Log.d(TAG, "Download completed successfully for song: ${songId.value}")

            // Send completion message to watch
            sendDownloadCompleteMessage(node.id, songId, true)

            // Cleanup
            cleanupDownload(songId, song)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)

            // Cleanup on error
            mutex.withLock {
                val song = _downloadingQueue.value.find { it.id == songId }
                if (song != null) {
                    _downloadingQueue.value = _downloadingQueue.value - song
                }
                downloadProgressMap.remove(songId)
                activeChannels.remove(songId)?.let { channel ->
                    try {
                        channelClient.close(channel).await()
                    } catch (closeEx: Exception) {
                        Log.e(TAG, "Error closing channel during error cleanup", closeEx)
                    }
                }
            }

            Result.failure(e)
        }
    }

    override suspend fun downloadPlaylist(
        playlistId: PlaylistId,
        quality: AudioQuality
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Starting playlist download: ${playlistId.value}")

            // Get playlist from repository
            val playlistResult = playlistRepository.getPlaylistById(playlistId)
            if (playlistResult.isFailure) {
                val error = "Failed to get playlist: ${playlistResult.exceptionOrNull()?.message}"
                Log.e(TAG, error)
                return Result.failure(playlistResult.exceptionOrNull() ?: Exception(error))
            }

            val playlist = playlistResult.getOrNull()
            if (playlist == null) {
                val error = "Playlist not found: ${playlistId.value}"
                Log.e(TAG, error)
                return Result.failure(IllegalStateException(error))
            }

            Log.d(TAG, "Found playlist: ${playlist.name} with ${playlist.getSongIds().size} songs")

            // Get all songs in the playlist
            val songs = mutableListOf<Song>()
            for (songId in playlist.getSongIds()) {
                val songResult = musicRepository.getSongById(songId)
                songResult.onSuccess { song ->
                    if (song != null) {
                        songs.add(song)
                    }
                }
            }

            if (songs.isEmpty()) {
                Log.w(TAG, "No songs found in playlist ${playlist.name}")
                return Result.success(Unit)
            }

            Log.d(TAG, "Downloading ${songs.size} songs from playlist ${playlist.name}")

            // Download each song sequentially
            var successCount = 0
            var failureCount = 0

            for (song in songs) {
                val result = downloadSong(song.id, quality)
                if (result.isSuccess) {
                    successCount++
                    Log.d(TAG, "Successfully downloaded ${song.title} ($successCount/${songs.size})")
                } else {
                    failureCount++
                    Log.e(TAG, "Failed to download ${song.title}: ${result.exceptionOrNull()?.message}")
                }
            }

            Log.d(TAG, "Playlist download completed. Success: $successCount, Failed: $failureCount")

            if (failureCount > 0 && successCount == 0) {
                Result.failure(Exception("Failed to download any songs from playlist"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Playlist download error", e)
            Result.failure(e)
        }
    }

    override suspend fun cancelDownload(songId: SongId): Result<Unit> {
        return try {
            Log.d(TAG, "Cancelling download for song: ${songId.value}")

            mutex.withLock {
                // Mark as cancelled
                cancelledDownloads.add(songId)
                pausedDownloads.remove(songId)

                // Close the active channel
                activeChannels.remove(songId)?.let { channel ->
                    try {
                        channelClient.close(channel).await()
                        Log.d(TAG, "Closed channel for cancelled download: ${songId.value}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error closing channel for cancelled download", e)
                    }
                }

                // Remove from downloading queue
                val song = _downloadingQueue.value.find { it.id == songId }
                if (song != null) {
                    _downloadingQueue.value = _downloadingQueue.value - song
                }

                // Remove progress tracking
                downloadProgressMap.remove(songId)
            }

            // Notify watch about cancellation
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isNotEmpty()) {
                sendDownloadCompleteMessage(nodes.first().id, songId, false)
            }

            Log.d(TAG, "Download cancelled successfully: ${songId.value}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling download", e)
            Result.failure(e)
        }
    }

    override suspend fun pauseDownload(songId: SongId): Result<Unit> {
        return try {
            Log.d(TAG, "Pausing download for song: ${songId.value}")

            mutex.withLock {
                if (!activeChannels.containsKey(songId)) {
                    Log.w(TAG, "No active download to pause for song: ${songId.value}")
                    return Result.success(Unit)
                }

                pausedDownloads.add(songId)
                Log.d(TAG, "Download paused: ${songId.value}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing download", e)
            Result.failure(e)
        }
    }

    override suspend fun resumeDownload(songId: SongId): Result<Unit> {
        return try {
            Log.d(TAG, "Resuming download for song: ${songId.value}")

            mutex.withLock {
                if (!pausedDownloads.contains(songId)) {
                    Log.w(TAG, "Download is not paused for song: ${songId.value}")
                    return Result.success(Unit)
                }

                pausedDownloads.remove(songId)
                Log.d(TAG, "Download resumed: ${songId.value}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming download", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteDownload(songId: SongId): Result<Unit> {
        // This is a phone-side implementation; deletion happens on watch side
        return Result.failure(UnsupportedOperationException("Delete download is a watch-side operation"))
    }

    override fun observeDownloadProgress(songId: SongId): Flow<Float> {
        return downloadProgressMap.getOrPut(songId) {
            MutableStateFlow(0f)
        }
    }

    override fun getDownloadingQueue(): Flow<List<Song>> {
        return _downloadingQueue
    }

    override suspend fun getAvailableStorage(): FileSize {
        // This is a phone-side implementation; storage info comes from watch
        return Result.failure<FileSize>(
            UnsupportedOperationException("Storage info is a watch-side operation")
        ).getOrDefault(FileSize.fromBytes(0))
    }

    override suspend fun getTotalDownloadedSize(): FileSize {
        // This is a phone-side implementation; download size comes from watch
        return Result.failure<FileSize>(
            UnsupportedOperationException("Download size is a watch-side operation")
        ).getOrDefault(FileSize.fromBytes(0))
    }

    override suspend fun cleanupOldDownloads(targetFreeSpace: FileSize): Result<Int> {
        // This is a phone-side implementation; cleanup happens on watch side
        return Result.failure(UnsupportedOperationException("Cleanup is a watch-side operation"))
    }

    /**
     * Helper function to download a file through a channel
     * Reads file in chunks and writes to output stream with progress tracking
     */
    private suspend fun downloadFile(
        channel: ChannelClient.Channel,
        file: File,
        songId: SongId
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting output stream from channel")
            val outputStream = channelClient.getOutputStream(channel).await()

            val fileSize = file.length()
            var totalBytesWritten = 0L
            val buffer = ByteArray(CHUNK_SIZE)

            Log.d(TAG, "Starting file download: ${file.name}, size: $fileSize bytes")

            FileInputStream(file).use { input ->
                var bytesRead: Int
                var lastProgressUpdate = 0f

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    // Check if download is cancelled
                    if (cancelledDownloads.contains(songId)) {
                        Log.d(TAG, "Download cancelled, stopping: ${songId.value}")
                        break
                    }

                    // Check if download is paused
                    while (pausedDownloads.contains(songId)) {
                        Log.d(TAG, "Download paused, waiting: ${songId.value}")
                        kotlinx.coroutines.delay(100) // Wait 100ms before checking again
                    }

                    // Write chunk to output stream
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead

                    // Calculate progress
                    val progress = (totalBytesWritten.toFloat() / fileSize.toFloat())

                    // Update progress every 5%
                    if (progress - lastProgressUpdate >= 0.05f || progress >= 1.0f) {
                        lastProgressUpdate = progress
                        mutex.withLock {
                            downloadProgressMap[songId]?.value = progress
                        }

                        // Send progress update to watch
                        sendProgressUpdate(songId, progress)

                        Log.d(TAG, "Download progress: ${(progress * 100).toInt()}% ($totalBytesWritten / $fileSize bytes)")
                    }

                    // Handle backpressure - flush periodically to avoid overwhelming the channel
                    if (totalBytesWritten % (CHUNK_SIZE * 10) == 0L) {
                        outputStream.flush()
                    }
                }

                // Final flush
                outputStream.flush()
                Log.d(TAG, "File download completed: $totalBytesWritten bytes written")
            }

            // Close the channel after successful download
            channelClient.close(channel).await()
            Log.d(TAG, "Channel closed successfully")

            // Check if was cancelled
            if (cancelledDownloads.contains(songId)) {
                Result.failure(Exception("Download was cancelled"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file", e)
            Result.failure(e)
        }
    }

    /**
     * Helper function to clean up download state
     */
    private suspend fun cleanupDownload(songId: SongId, song: Song) {
        mutex.withLock {
            _downloadingQueue.value = _downloadingQueue.value - song
            downloadProgressMap.remove(songId)
            activeChannels.remove(songId)
            pausedDownloads.remove(songId)
            cancelledDownloads.remove(songId)
        }
    }

    /**
     * Send download start message to watch
     */
    private suspend fun sendDownloadStartMessage(
        nodeId: String,
        songId: SongId,
        quality: AudioQuality,
        fileSize: Long
    ) {
        try {
            val message = DownloadStartMessage(
                songId = songId.value,
                quality = quality.name,
                fileSize = fileSize
            )
            val messageJson = json.encodeToString(message)
            messageClient.sendMessage(
                nodeId,
                WearPaths.DOWNLOAD_REQUEST,
                messageJson.toByteArray()
            ).await()
            Log.d(TAG, "Sent download start message for song: ${songId.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending download start message", e)
        }
    }

    /**
     * Send progress update to watch
     */
    private suspend fun sendProgressUpdate(songId: SongId, progress: Float) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) return

            val message = DownloadProgressMessage(
                songId = songId.value,
                progress = progress
            )
            val messageJson = json.encodeToString(message)
            messageClient.sendMessage(
                nodes.first().id,
                WearPaths.DOWNLOAD_PROGRESS,
                messageJson.toByteArray()
            ).await()
        } catch (e: Exception) {
            // Don't log progress update failures to avoid spam
            // Only log if it's a significant error
            if (e !is java.io.IOException) {
                Log.e(TAG, "Error sending progress update", e)
            }
        }
    }

    /**
     * Send download complete message to watch
     */
    private suspend fun sendDownloadCompleteMessage(
        nodeId: String,
        songId: SongId,
        success: Boolean
    ) {
        try {
            val message = DownloadCompleteMessage(
                songId = songId.value,
                success = success
            )
            val messageJson = json.encodeToString(message)
            messageClient.sendMessage(
                nodeId,
                WearPaths.DOWNLOAD_COMPLETE,
                messageJson.toByteArray()
            ).await()
            Log.d(TAG, "Sent download complete message for song: ${songId.value}, success: $success")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending download complete message", e)
        }
    }
}

/**
 * Message DTOs for download operations
 */
@Serializable
private data class DownloadStartMessage(
    val songId: String,
    val quality: String,
    val fileSize: Long
)

@Serializable
private data class DownloadProgressMessage(
    val songId: String,
    val progress: Float
)

@Serializable
private data class DownloadCompleteMessage(
    val songId: String,
    val success: Boolean
)


