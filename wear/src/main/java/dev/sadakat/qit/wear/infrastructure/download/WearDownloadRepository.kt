package dev.sadakat.qit.wear.infrastructure.download

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.FileSize
import dev.sadakat.qit.shared.dto.DownloadRequestMessage
import dev.sadakat.qit.wear.data.local.dao.PlaylistDao
import dev.sadakat.qit.wear.data.local.dao.SongDao
import dev.sadakat.qit.wear.infrastructure.storage.StorageManager
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext

/**
 * Watch-side implementation of DownloadRepository
 * Handles receiving downloaded files from phone via Wearable Channel API
 *
 * Concurrency model (P11): all mutable maps/sets below are guarded by
 * [stateMutex]. Entry points invoked from binder threads
 * (handleDownloadStart/Progress/Complete) hop into [scope] and take the lock.
 * Deduplication (already-downloading check + registration) is a single
 * critical section, so a double-tap can never produce two channels writing
 * two FileOutputStreams to the same path.
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

    private val json = Json { ignoreUnknownKeys = true }

    // All guarded by stateMutex:
    private val stateMutex = Mutex()

    /**
     * Download progress per song. Instance-stable: collectors hold it.
     * ConcurrentHashMap: observeDownloadProgress() creates entries from the
     * caller's thread (not suspend, cannot take the mutex); Kotlin's
     * ConcurrentMap.getOrPut is atomic (putIfAbsent).
     */
    private val downloadProgressMap = ConcurrentHashMap<SongId, MutableStateFlow<Float>>()

    /** Active (running) transfer jobs, keyed by song. */
    private val activeDownloads = mutableMapOf<SongId, Job>()

    /**
     * Channel of the most recent (re)registration per song. The channel path
     * only carries the song id, so a late close of a STALE channel must be
     * filtered by IDENTITY (same model as WearStreamingRepository) or it
     * would strip a newer retry's registration.
     */
    private val activeChannels = mutableMapOf<SongId, ChannelClient.Channel>()

    /** Songs whose request was sent but whose channel has not opened yet. */
    private val pendingRequests = mutableSetOf<SongId>()

    /** File sizes announced by the phone (DownloadStartMessage). */
    private val expectedFileSizes = mutableMapOf<SongId, Long>()

    private val pausedDownloads = mutableSetOf<SongId>()

    /** Watchdogs clearing pending state when the phone never opens a channel (P10). */
    private val requestWatchdogs = mutableMapOf<SongId, Job>()

    private val _downloadingQueue = MutableStateFlow<List<Song>>(emptyList())

    // Listener for incoming download channels
    private val channelCallback = object : ChannelClient.ChannelCallback() {
        override fun onChannelOpened(channel: ChannelClient.Channel) {
            Log.d(TAG, "Channel opened: ${channel.path}")

            // Check if this is a download channel
            if (channel.path.startsWith(WearPaths.DOWNLOAD_CHANNEL)) {
                val songId = channel.path.removePrefix(WearPaths.DOWNLOAD_CHANNEL)
                if (songId.isNotBlank()) {
                    scope.launch {
                        handleIncomingDownload(channel, SongId.from(songId))
                    }
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
                        val wasTracked = stateMutex.withLock {
                            // Identity check: an abnormal close of a channel we
                            // are no longer tracking belongs to a superseded
                            // attempt - the newer download's state must survive.
                            if (activeChannels[songId] !== channel) {
                                false
                            } else {
                                activeChannels.remove(songId)
                                activeDownloads.remove(songId)?.cancel()
                                pendingRequests.remove(songId)
                                requestWatchdogs.remove(songId)?.cancel()
                                downloadProgressMap.remove(songId)
                                true
                            }
                        }
                        if (wasTracked) {
                            updateDownloadingQueue()
                        }
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
        quality: AudioQuality,
        sourceNodeId: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Requesting download from phone: songId=$songId, quality=$quality")

            // Check if already downloaded
            val existingSong = songDao.getSongById(songId.value)
            if (existingSong?.isDownloaded == true) {
                Log.d(TAG, "Song already downloaded: $songId")
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

            // Dedupe + register as ONE critical section (P11): a concurrent
            // second tap must see the pending request and back off BEFORE a
            // second message/channel is created.
            val registered = stateMutex.withLock {
                if (activeDownloads.containsKey(songId) || pendingRequests.contains(songId)) {
                    false
                } else {
                    // Never REPLACE the progress flow instance - collectors
                    // hold a reference and would be orphaned.
                    downloadProgressMap.getOrPut(songId) { MutableStateFlow(0f) }.value = 0f
                    pendingRequests.add(songId)
                    true
                }
            }
            if (!registered) {
                Log.d(TAG, "Song already downloading: $songId")
                return@withContext Result.success(Unit)
            }

            try {
                // Update song status to downloading
                songDao.updateDownloadProgress(songId.value, 0f)
                updateDownloadingQueue()

                // Send download request to phone (must be JSON - the phone parses
                // it as a DownloadRequestMessage)
                val request = DownloadRequestMessage(
                    songId = songId.value,
                    quality = quality.name
                )
                val requestData = json.encodeToString(request).toByteArray()
                val node = nodes.first()
                messageClient.sendMessage(
                    node.id,
                    WearPaths.DOWNLOAD_REQUEST,
                    requestData
                ).await()

                // P10: if the phone silently fails (song/file missing, node
                // dropped), no channel ever opens. The watchdog clears the
                // pending state so the UI is not stuck forever.
                startRequestWatchdog(songId)

                Log.d(TAG, "Download request sent to phone for song: $songId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send download request for song: $songId", e)
                stateMutex.withLock {
                    pendingRequests.remove(songId)
                    requestWatchdogs.remove(songId)?.cancel()
                    downloadProgressMap.remove(songId)
                }
                Result.failure(e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request download from phone", e)
            Result.failure(e)
        }
    }

    /**
     * Starts (or replaces) the request watchdog for [songId]: after
     * [DOWNLOAD_REQUEST_TIMEOUT_MS] without a channel being opened, the
     * pending state is cleared so the song can be re-requested.
     */
    private suspend fun startRequestWatchdog(songId: SongId) {
        val watchdog = scope.launch {
            delay(DOWNLOAD_REQUEST_TIMEOUT_MS)
            val timedOut = stateMutex.withLock {
                requestWatchdogs.remove(songId)
                if (!pendingRequests.contains(songId)) {
                    false
                } else {
                    Log.w(TAG, "Download request timed out for song: $songId (no channel opened)")
                    pendingRequests.remove(songId)
                    activeDownloads.remove(songId)
                    activeChannels.remove(songId)
                    downloadProgressMap.remove(songId)
                    true
                }
            }
            if (timedOut) {
                try {
                    songDao.updateDownloadProgress(songId.value, 0f)
                    updateDownloadingQueue()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to reset progress after watchdog timeout", e)
                }
            }
        }
        stateMutex.withLock {
            requestWatchdogs[songId]?.cancel()
            requestWatchdogs[songId] = watchdog
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download playlist", e)
            Result.failure(e)
        }
    }

    override suspend fun cancelDownload(songId: SongId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Canceling download: $songId")

            stateMutex.withLock {
                // Cancel active job
                activeDownloads.remove(songId)?.cancel()
                activeChannels.remove(songId)
                pendingRequests.remove(songId)
                requestWatchdogs.remove(songId)?.cancel()
                pausedDownloads.remove(songId)

                // Clear progress
                downloadProgressMap.remove(songId)
            }

            // Reset download status in database
            songDao.updateDownloadProgress(songId.value, 0f)
            updateDownloadingQueue()

            // Delete partial file if any
            val partialFile = File(File(context.filesDir, DOWNLOAD_DIR), "${songId.value}.mp3")
            if (partialFile.exists()) {
                partialFile.delete()
            }

            // Send cancellation message to phone
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isNotEmpty()) {
                    val cancelData = songId.value.toByteArray()
                    messageClient.sendMessage(
                        nodes.first().id,
                        WearPaths.DOWNLOAD_CANCEL,
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

            stateMutex.withLock {
                // Mark as paused
                pausedDownloads.add(songId)

                // Cancel active job (will be resumed later)
                activeDownloads.remove(songId)?.cancel()
            }

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

            val wasPaused = stateMutex.withLock {
                if (pausedDownloads.contains(songId)) {
                    pausedDownloads.remove(songId)
                    true
                } else false
            }
            if (!wasPaused) {
                return@withContext Result.failure(Exception("Download is not paused"))
            }

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
            val wasActive = stateMutex.withLock { activeDownloads.containsKey(songId) }
            if (wasActive) {
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
        // getOrPut on a ConcurrentMap is atomic - safe without stateMutex
        // (this accessor is not suspend and runs on the caller's thread).
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
     * Handle incoming download from phone via channel.
     *
     * Registers the transfer job in [activeDownloads] BEFORE its body can run
     * (LAZY start): a job that completes before its assignment would leave a
     * completed Job in the map, permanently blocking re-downloads.
     */
    private suspend fun handleIncomingDownload(
        channel: ChannelClient.Channel,
        songId: SongId
    ) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            receiveDownload(channel, songId)
        }
        stateMutex.withLock {
            requestWatchdogs.remove(songId)?.cancel()
            pendingRequests.remove(songId)
            activeDownloads[songId] = job
            activeChannels[songId] = channel
        }
        updateDownloadingQueue()
        job.start()
    }

    /**
     * Reads the channel input stream into the song's download file.
     */
    private suspend fun receiveDownload(
        channel: ChannelClient.Channel,
        songId: SongId
    ) {
        val selfJob = coroutineContext[Job]
        val file = File(File(context.filesDir, DOWNLOAD_DIR), "${songId.value}.mp3")
        try {
            Log.d(TAG, "Handling incoming download for song: $songId")

            // Check if paused
            if (stateMutex.withLock { pausedDownloads.contains(songId) }) {
                Log.d(TAG, "Download is paused, closing channel: $songId")
                channelClient.close(channel).await()
                return
            }

            // Get input stream from channel
            val inputStream = channelClient.getInputStream(channel).await()

            // Create download directory if not exists
            val downloadDir = File(context.filesDir, DOWNLOAD_DIR)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            // Read from channel and write to file
            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var lastProgressUpdate = 0L
            val expectedSize = stateMutex.withLock {
                expectedFileSizes[songId] ?: ESTIMATED_SONG_SIZE_BYTES
            }

            FileOutputStream(file).use { output ->
                inputStream.use { input ->
                    while (isJobActive(selfJob) && !stateMutex.withLock { pausedDownloads.contains(songId) }) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) {
                            Log.d(TAG, "Reached end of download stream")
                            break
                        }

                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Update progress based on the file size reported by
                        // the phone (falls back to an estimate)
                        val progress = (totalBytesRead.toFloat() / expectedSize)
                            .coerceIn(0f, 0.99f)

                        // Only update every 100KB to avoid too many updates
                        if (totalBytesRead - lastProgressUpdate > 100_000) {
                            stateMutex.withLock {
                                downloadProgressMap[songId]?.value = progress
                            }
                            songDao.updateDownloadProgress(songId.value, progress)
                            // Re-emit the queue so the UI shows live progress (P22)
                            updateDownloadingQueue()
                            lastProgressUpdate = totalBytesRead
                            Log.d(TAG, "Download progress: ${(progress * 100).toInt()}% ($totalBytesRead bytes)")
                        }
                    }
                }
            }

            // Check if download was completed (not paused or cancelled)
            if (isJobActive(selfJob) && !stateMutex.withLock { pausedDownloads.contains(songId) }) {
                Log.d(TAG, "Download completed: $songId, size: $totalBytesRead bytes")

                // Update database - mark as downloaded
                songDao.updateDownloadStatus(songId.value, true, file.absolutePath)
                songDao.updateDownloadProgress(songId.value, 1f)

                // Update progress to 100%
                stateMutex.withLock {
                    downloadProgressMap[songId]?.value = 1f
                    // Remove by identity: a newer job for the same song may
                    // already be registered.
                    if (activeDownloads[songId] === selfJob) {
                        activeDownloads.remove(songId)
                    }
                    downloadProgressMap.remove(songId)
                }
                updateDownloadingQueue()

                Log.d(TAG, "Song successfully downloaded to: ${file.absolutePath}")
            } else {
                // Download was cancelled or paused, delete partial file
                Log.d(TAG, "Download cancelled or paused, deleting partial file")
                file.delete()
            }
        } catch (e: CancellationException) {
            // Job cancelled (pause/cancel/delete/superseded): rethrow so the
            // coroutine machinery completes as cancelled. State cleanup is
            // done by the caller that cancelled us or the finally block.
            Log.d(TAG, "Download job cancelled for song: $songId")
            if (file.exists()) file.delete()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming download", e)

            // Clean up on error
            stateMutex.withLock {
                if (activeDownloads[songId] === selfJob) {
                    activeDownloads.remove(songId)
                }
                pendingRequests.remove(songId)
                downloadProgressMap.remove(songId)
            }
            updateDownloadingQueue()

            // Delete partial file
            if (file.exists()) {
                file.delete()
            }
        } finally {
            // Belt & braces: make sure this job is never stuck in the map.
            stateMutex.withLock {
                if (activeDownloads[songId] === selfJob) {
                    activeDownloads.remove(songId)
                    downloadProgressMap.remove(songId)
                }
                if (activeChannels[songId] === channel) {
                    activeChannels.remove(songId)
                }
            }
        }
    }

    private fun isJobActive(job: Job?): Boolean = job?.isActive != false

    /**
     * Update the downloading queue state
     */
    private suspend fun updateDownloadingQueue() {
        try {
            val downloadingSongIds = stateMutex.withLock { activeDownloads.keys.map { it.value } }
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
     * Handle a progress update pushed from the phone (DownloadProgressMessage JSON).
     * Invoked on a binder thread: hops into [scope] and takes [stateMutex].
     */
    fun handleDownloadProgress(songId: SongId, progress: Float) {
        scope.launch {
            val clamped = progress.coerceIn(0f, 1f)
            stateMutex.withLock {
                downloadProgressMap[songId]?.value = clamped
            }
            try {
                songDao.updateDownloadProgress(songId.value, clamped)
                // Re-emit the queue so the UI shows live progress (P22)
                updateDownloadingQueue()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist download progress", e)
            }
        }
    }

    /**
     * Handle the download start notification from the phone, which carries the
     * real file size so progress can be computed accurately.
     * Invoked on a binder thread: hops into [scope] and takes [stateMutex].
     */
    fun handleDownloadStart(songId: SongId, fileSize: Long) {
        scope.launch {
            if (fileSize > 0) {
                stateMutex.withLock {
                    expectedFileSizes[songId] = fileSize
                }
            }
        }
    }

    /**
     * Handle the download completion notification from the phone.
     * A failure notification aborts the active transfer and cleans up state.
     * Invoked on a binder thread: hops into [scope] and takes [stateMutex].
     */
    fun handleDownloadComplete(songId: SongId, success: Boolean) {
        if (!success) {
            Log.w(TAG, "Phone reported download failure for song: $songId")
            scope.launch {
                stateMutex.withLock {
                    activeChannels.remove(songId)
                    activeDownloads.remove(songId)?.cancel()
                    pendingRequests.remove(songId)
                    requestWatchdogs.remove(songId)?.cancel()
                    downloadProgressMap.remove(songId)
                }
                try {
                    songDao.updateDownloadProgress(songId.value, 0f)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to reset progress after failure", e)
                }
                updateDownloadingQueue()

                // Delete partial file
                val partialFile = File(File(context.filesDir, DOWNLOAD_DIR), "${songId.value}.mp3")
                if (partialFile.exists()) {
                    partialFile.delete()
                }
            }
        }
        // On success, receiveDownload finalizes the download when the
        // channel input stream reaches its end.
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        channelClient.unregisterChannelCallback(channelCallback)
        runBlocking {
            stateMutex.withLock {
                activeDownloads.values.forEach { it.cancel() }
                activeDownloads.clear()
                activeChannels.clear()
                pendingRequests.clear()
                requestWatchdogs.clear()
                downloadProgressMap.clear()
            }
        }
        scope.cancel()
    }

    companion object {
        private const val TAG = "WearDownloadRepo"
        private const val DOWNLOAD_DIR = "downloads"
        private const val MIN_REQUIRED_STORAGE_BYTES = 10 * 1024 * 1024L // 10 MB
        private const val ESTIMATED_SONG_SIZE_BYTES = 5 * 1024 * 1024L // 5 MB average

        /**
         * How long to wait for the phone to open a download channel after the
         * request was sent before giving up (P10).
         */
        private const val DOWNLOAD_REQUEST_TIMEOUT_MS = 60_000L
    }
}
