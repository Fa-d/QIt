package dev.sadakat.qit.shared.domain.repository

import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for Phone-Watch synchronization
 */
interface SyncRepository {

    /**
     * Syncs a playlist to the watch
     */
    suspend fun syncPlaylistToWatch(playlist: Playlist): Result<Unit>

    /**
     * Syncs multiple playlists to the watch
     */
    suspend fun syncPlaylistsToWatch(playlists: List<Playlist>): Result<Unit>

    /**
     * Syncs a song metadata to the watch
     */
    suspend fun syncSongToWatch(song: Song): Result<Unit>

    /**
     * Syncs multiple songs metadata to the watch
     */
    suspend fun syncSongsToWatch(songs: List<Song>): Result<Unit>

    /**
     * Requests playlist sync from phone (watch-side)
     */
    suspend fun requestPlaylistSyncFromPhone(): Result<Unit>

    /**
     * Requests song sync from phone (watch-side)
     */
    suspend fun requestSongSyncFromPhone(playlistId: PlaylistId): Result<Unit>

    /**
     * Checks if watch is connected
     */
    suspend fun isWatchConnected(): Boolean

    /**
     * Observes watch connection status
     */
    fun observeWatchConnection(): Flow<Boolean>

    /**
     * Sends a playback command to watch
     */
    suspend fun sendPlaybackCommand(
        command: String,
        songId: SongId? = null
    ): Result<Unit>

    /**
     * Gets the last sync timestamp
     */
    suspend fun getLastSyncTimestamp(): Long

    /**
     * Updates the last sync timestamp
     */
    suspend fun updateLastSyncTimestamp(timestamp: Long): Result<Unit>
}
