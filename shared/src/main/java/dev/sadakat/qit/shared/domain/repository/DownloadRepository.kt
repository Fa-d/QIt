package dev.sadakat.qit.shared.domain.repository

import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.FileSize
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for download operations (watch-side)
 */
interface DownloadRepository {

    /**
     * Downloads a song to watch storage
     */
    suspend fun downloadSong(
        songId: SongId,
        quality: AudioQuality = AudioQuality.MEDIUM
    ): Result<Unit>

    /**
     * Downloads an entire playlist to watch storage
     */
    suspend fun downloadPlaylist(
        playlistId: PlaylistId,
        quality: AudioQuality = AudioQuality.MEDIUM
    ): Result<Unit>

    /**
     * Cancels an ongoing download
     */
    suspend fun cancelDownload(songId: SongId): Result<Unit>

    /**
     * Pauses an ongoing download
     */
    suspend fun pauseDownload(songId: SongId): Result<Unit>

    /**
     * Resumes a paused download
     */
    suspend fun resumeDownload(songId: SongId): Result<Unit>

    /**
     * Deletes a downloaded song from watch storage
     */
    suspend fun deleteDownload(songId: SongId): Result<Unit>

    /**
     * Observes download progress for a song
     */
    fun observeDownloadProgress(songId: SongId): Flow<Float>

    /**
     * Gets all currently downloading songs
     */
    fun getDownloadingQueue(): Flow<List<Song>>

    /**
     * Gets available storage space on watch
     */
    suspend fun getAvailableStorage(): FileSize

    /**
     * Gets total storage used by downloads
     */
    suspend fun getTotalDownloadedSize(): FileSize

    /**
     * Cleans up old downloads based on strategy (LRU, oldest, etc.)
     */
    suspend fun cleanupOldDownloads(targetFreeSpace: FileSize): Result<Int>
}
