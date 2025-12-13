package dev.sadakat.qit.wear.infrastructure.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.FileSize
import dev.sadakat.qit.wear.data.local.dao.SongDao
import dev.sadakat.qit.wear.data.local.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages storage for downloaded songs on the watch
 * Handles storage checks, cleanup, and LRU eviction
 */
@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) {

    /**
     * Get available storage space on the watch
     */
    suspend fun getAvailableStorage(): FileSize = withContext(Dispatchers.IO) {
        try {
            val stat = StatFs(context.filesDir.absolutePath)
            val availableBytes = stat.availableBytes
            FileSize.fromBytes(availableBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get available storage", e)
            FileSize.ZERO
        }
    }

    /**
     * Get total storage space on the watch
     */
    suspend fun getTotalStorage(): FileSize = withContext(Dispatchers.IO) {
        try {
            val stat = StatFs(context.filesDir.absolutePath)
            val totalBytes = stat.totalBytes
            FileSize.fromBytes(totalBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get total storage", e)
            FileSize.ZERO
        }
    }

    /**
     * Get total size of all downloaded songs
     */
    suspend fun getTotalDownloadedSize(): FileSize = withContext(Dispatchers.IO) {
        try {
            val downloadDir = File(context.filesDir, DOWNLOAD_DIR)
            if (!downloadDir.exists()) {
                return@withContext FileSize.ZERO
            }

            var totalSize = 0L
            downloadDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                }
            }

            FileSize.fromBytes(totalSize)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate total downloaded size", e)
            FileSize.ZERO
        }
    }

    /**
     * Get list of all downloaded songs with their file sizes
     */
    suspend fun getDownloadedSongs(): List<DownloadedSongInfo> = withContext(Dispatchers.IO) {
        try {
            val downloadedSongs = mutableListOf<DownloadedSongInfo>()

            // Get all downloaded songs from database
            val songs = songDao.getAllSongs().first()
            val downloaded = songs.filter { songEntity -> songEntity.isDownloaded && songEntity.localFilePath != null }

            for (songEntity in downloaded) {
                val file = File(songEntity.localFilePath!!)
                if (file.exists()) {
                    downloadedSongs.add(
                        DownloadedSongInfo(
                            songId = SongId.from(songEntity.id),
                            filePath = songEntity.localFilePath,
                            fileSize = FileSize.fromBytes(file.length()),
                            lastAccessTime = file.lastModified(),
                            dateAdded = songEntity.dateAdded
                        )
                    )
                } else {
                    // File doesn't exist, update database
                    Log.w(TAG, "Downloaded song file not found: ${songEntity.id}, cleaning up database")
                    songDao.updateDownloadStatus(songEntity.id, false, null)
                }
            }

            downloadedSongs
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get downloaded songs", e)
            emptyList()
        }
    }

    /**
     * Delete a downloaded song from storage
     */
    suspend fun deleteSong(songId: SongId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting song: $songId")

            // Get song from database
            val song = songDao.getSongById(songId.value)

            if (song?.localFilePath != null) {
                val file = File(song.localFilePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted) {
                        Log.d(TAG, "Successfully deleted file: ${file.absolutePath}")
                    } else {
                        Log.w(TAG, "Failed to delete file: ${file.absolutePath}")
                    }
                }
            }

            // Update database
            songDao.updateDownloadStatus(songId.value, false, null)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete song", e)
            Result.failure(e)
        }
    }

    /**
     * Delete multiple songs from storage
     */
    suspend fun deleteSongs(songIds: List<SongId>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var deletedCount = 0

            songIds.forEach { songId ->
                val result = deleteSong(songId)
                if (result.isSuccess) {
                    deletedCount++
                }
            }

            Log.d(TAG, "Deleted $deletedCount out of ${songIds.size} songs")
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete songs", e)
            Result.failure(e)
        }
    }

    /**
     * Clean up old downloads using LRU (Least Recently Used) strategy
     * Deletes oldest accessed files until target free space is achieved
     *
     * @param targetFreeSpace The desired amount of free space
     * @return Number of songs deleted
     */
    suspend fun cleanupOldDownloads(targetFreeSpace: FileSize): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting cleanup to achieve ${targetFreeSpace.format()} free space")

            val currentAvailable = getAvailableStorage()
            if (currentAvailable >= targetFreeSpace) {
                Log.d(TAG, "Already have enough space: ${currentAvailable.format()}")
                return@withContext 0
            }

            val neededSpace = targetFreeSpace.bytes - currentAvailable.bytes
            Log.d(TAG, "Need to free up: ${FileSize.fromBytes(neededSpace).format()}")

            // Get all downloaded songs sorted by last access time (oldest first)
            val downloadedSongs = getDownloadedSongs()
                .sortedBy { it.lastAccessTime }

            var freedSpace = 0L
            var deletedCount = 0

            // Delete songs until we have enough space
            for (songInfo in downloadedSongs) {
                if (freedSpace >= neededSpace) {
                    break
                }

                Log.d(TAG, "Deleting song: ${songInfo.songId} (${songInfo.fileSize.format()})")
                val result = deleteSong(songInfo.songId)

                if (result.isSuccess) {
                    freedSpace += songInfo.fileSize.bytes
                    deletedCount++
                }
            }

            Log.d(TAG, "Cleanup complete: deleted $deletedCount songs, freed ${FileSize.fromBytes(freedSpace).format()}")

            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old downloads", e)
            0
        }
    }

    /**
     * Clean up downloads based on age (delete songs older than specified days)
     */
    suspend fun cleanupByAge(maxAgeDays: Int): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Cleaning up downloads older than $maxAgeDays days")

            val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
            val downloadedSongs = getDownloadedSongs()

            val oldSongs = downloadedSongs.filter { it.dateAdded < cutoffTime }
            val songIds = oldSongs.map { it.songId }

            if (songIds.isEmpty()) {
                Log.d(TAG, "No old songs to delete")
                return@withContext 0
            }

            val result = deleteSongs(songIds)
            val deletedCount = result.getOrNull() ?: 0

            Log.d(TAG, "Deleted $deletedCount songs older than $maxAgeDays days")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup by age", e)
            0
        }
    }

    /**
     * Clean up based on storage quota
     * Ensures downloaded songs don't exceed specified quota
     */
    suspend fun enforceStorageQuota(maxQuota: FileSize): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Enforcing storage quota: ${maxQuota.format()}")

            val currentSize = getTotalDownloadedSize()

            if (currentSize <= maxQuota) {
                Log.d(TAG, "Within quota: ${currentSize.format()} / ${maxQuota.format()}")
                return@withContext 0
            }

            val excessSize = currentSize.bytes - maxQuota.bytes
            Log.d(TAG, "Over quota by: ${FileSize.fromBytes(excessSize).format()}")

            // Get songs sorted by last access time (oldest first)
            val downloadedSongs = getDownloadedSongs()
                .sortedBy { it.lastAccessTime }

            var freedSpace = 0L
            var deletedCount = 0

            // Delete oldest songs until within quota
            for (songInfo in downloadedSongs) {
                if (freedSpace >= excessSize) {
                    break
                }

                val result = deleteSong(songInfo.songId)
                if (result.isSuccess) {
                    freedSpace += songInfo.fileSize.bytes
                    deletedCount++
                }
            }

            Log.d(TAG, "Quota enforcement complete: deleted $deletedCount songs, freed ${FileSize.fromBytes(freedSpace).format()}")

            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enforce storage quota", e)
            0
        }
    }

    /**
     * Check if there's enough storage for a download
     */
    suspend fun hasEnoughStorageFor(estimatedSize: FileSize): Boolean = withContext(Dispatchers.IO) {
        try {
            val available = getAvailableStorage()
            val required = estimatedSize.bytes + MIN_REQUIRED_FREE_SPACE_BYTES

            available.bytes >= required
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check storage availability", e)
            false
        }
    }

    /**
     * Get storage statistics
     */
    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        try {
            val total = getTotalStorage()
            val available = getAvailableStorage()
            val downloaded = getTotalDownloadedSize()
            val downloadedCount = getDownloadedSongs().size

            StorageStats(
                totalStorage = total,
                availableStorage = available,
                usedStorage = FileSize.fromBytes(total.bytes - available.bytes),
                downloadedSize = downloaded,
                downloadedCount = downloadedCount,
                downloadedPercentage = if (total.bytes > 0) {
                    (downloaded.bytes.toFloat() / total.bytes.toFloat()) * 100f
                } else 0f
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get storage stats", e)
            StorageStats(
                totalStorage = FileSize.ZERO,
                availableStorage = FileSize.ZERO,
                usedStorage = FileSize.ZERO,
                downloadedSize = FileSize.ZERO,
                downloadedCount = 0,
                downloadedPercentage = 0f
            )
        }
    }

    /**
     * Clean up orphaned files (files without database entries)
     */
    suspend fun cleanupOrphanedFiles(): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Cleaning up orphaned files")

            val downloadDir = File(context.filesDir, DOWNLOAD_DIR)
            if (!downloadDir.exists()) {
                return@withContext 0
            }

            // Get all song IDs from database
            val allSongs = songDao.getAllSongs().first()
            val validSongIds = allSongs
                .filter { it.isDownloaded && it.localFilePath != null }
                .map { song -> File(song.localFilePath!!).name }
                .toSet()

            var deletedCount = 0

            // Delete files that don't have a database entry
            downloadDir.listFiles()?.forEach { file ->
                if (file.isFile && !validSongIds.contains(file.name)) {
                    Log.d(TAG, "Deleting orphaned file: ${file.name}")
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }

            Log.d(TAG, "Cleaned up $deletedCount orphaned files")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup orphaned files", e)
            0
        }
    }

    companion object {
        private const val TAG = "StorageManager"
        private const val DOWNLOAD_DIR = "downloads"
        private const val MIN_REQUIRED_FREE_SPACE_BYTES = 50 * 1024 * 1024L // 50 MB
    }
}

/**
 * Information about a downloaded song
 */
data class DownloadedSongInfo(
    val songId: SongId,
    val filePath: String,
    val fileSize: FileSize,
    val lastAccessTime: Long,
    val dateAdded: Long
)

/**
 * Storage statistics
 */
data class StorageStats(
    val totalStorage: FileSize,
    val availableStorage: FileSize,
    val usedStorage: FileSize,
    val downloadedSize: FileSize,
    val downloadedCount: Int,
    val downloadedPercentage: Float
) {
    fun format(): String {
        return """
            Total Storage: ${totalStorage.format()}
            Available: ${availableStorage.format()}
            Used: ${usedStorage.format()}
            Downloaded: ${downloadedSize.format()} ($downloadedCount songs, ${String.format("%.1f", downloadedPercentage)}%)
        """.trimIndent()
    }
}
