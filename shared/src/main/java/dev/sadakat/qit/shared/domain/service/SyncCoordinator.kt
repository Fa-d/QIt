package dev.sadakat.qit.shared.domain.service

import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.repository.SyncRepository

/**
 * Domain Service for coordinating sync operations between phone and watch
 */
class SyncCoordinator(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository,
    private val syncRepository: SyncRepository
) {

    /**
     * Syncs all playlists and their songs to the watch
     */
    suspend fun syncAllToWatch(): Result<SyncResult> {
        return try {
            if (!syncRepository.isWatchConnected()) {
                return Result.failure(IllegalStateException("Watch not connected"))
            }

            var playlistsSynced = 0
            var songsSynced = 0
            val errors = mutableListOf<String>()

            // Get all playlists
            val allPlaylists = mutableListOf<Playlist>()
            playlistRepository.getAllPlaylists().collect { playlists ->
                allPlaylists.clear()
                allPlaylists.addAll(playlists)
            }

            // Sync each playlist
            for (playlist in allPlaylists) {
                val result = syncRepository.syncPlaylistToWatch(playlist)
                if (result.isSuccess) {
                    playlistsSynced++

                    // Sync songs for this playlist
                    val songsResult = playlistRepository.getSongsForPlaylist(playlist.id)
                    val songs = songsResult.getOrNull() ?: emptyList()

                    if (songs.isNotEmpty()) {
                        val syncSongsResult = syncRepository.syncSongsToWatch(songs)
                        if (syncSongsResult.isSuccess) {
                            songsSynced += songs.size
                        } else {
                            errors.add("Failed to sync songs for ${playlist.name}")
                        }
                    }
                } else {
                    errors.add("Failed to sync playlist ${playlist.name}")
                }
            }

            // Update last sync timestamp
            syncRepository.updateLastSyncTimestamp(System.currentTimeMillis())

            Result.success(
                SyncResult(
                    playlistsSynced = playlistsSynced,
                    songsSynced = songsSynced,
                    errors = errors
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Syncs a specific playlist and its songs to the watch
     */
    suspend fun syncPlaylistToWatch(playlistId: PlaylistId): Result<Unit> {
        return try {
            if (!syncRepository.isWatchConnected()) {
                return Result.failure(IllegalStateException("Watch not connected"))
            }

            // Get playlist
            val playlistResult = playlistRepository.getPlaylistById(playlistId)
            val playlist = playlistResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Playlist not found"))

            // Sync playlist metadata
            syncRepository.syncPlaylistToWatch(playlist).getOrElse {
                return Result.failure(it)
            }

            // Sync songs
            val songsResult = playlistRepository.getSongsForPlaylist(playlistId)
            val songs = songsResult.getOrNull() ?: emptyList()

            if (songs.isNotEmpty()) {
                syncRepository.syncSongsToWatch(songs)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Performs delta sync - only syncs changes since last sync
     */
    suspend fun performDeltaSync(): Result<SyncResult> {
        return try {
            if (!syncRepository.isWatchConnected()) {
                return Result.failure(IllegalStateException("Watch not connected"))
            }

            val lastSyncTimestamp = syncRepository.getLastSyncTimestamp()
            var playlistsSynced = 0
            var songsSynced = 0
            val errors = mutableListOf<String>()

            // Get all playlists
            val allPlaylists = mutableListOf<Playlist>()
            playlistRepository.getAllPlaylists().collect { playlists ->
                allPlaylists.clear()
                allPlaylists.addAll(playlists)
            }

            // Only sync playlists updated after last sync
            val updatedPlaylists = allPlaylists.filter { it.updatedAt > lastSyncTimestamp }

            for (playlist in updatedPlaylists) {
                val result = syncRepository.syncPlaylistToWatch(playlist)
                if (result.isSuccess) {
                    playlistsSynced++

                    // Sync songs for this playlist
                    val songsResult = playlistRepository.getSongsForPlaylist(playlist.id)
                    val songs = songsResult.getOrNull() ?: emptyList()

                    if (songs.isNotEmpty()) {
                        val syncSongsResult = syncRepository.syncSongsToWatch(songs)
                        if (syncSongsResult.isSuccess) {
                            songsSynced += songs.size
                        } else {
                            errors.add("Failed to sync songs for ${playlist.name}")
                        }
                    }
                } else {
                    errors.add("Failed to sync playlist ${playlist.name}")
                }
            }

            // Update last sync timestamp
            syncRepository.updateLastSyncTimestamp(System.currentTimeMillis())

            Result.success(
                SyncResult(
                    playlistsSynced = playlistsSynced,
                    songsSynced = songsSynced,
                    errors = errors
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if sync is needed (based on last sync timestamp)
     */
    suspend fun isSyncNeeded(): Boolean {
        val lastSync = syncRepository.getLastSyncTimestamp()
        val now = System.currentTimeMillis()
        val fiveMinutes = 5 * 60 * 1000L

        return (now - lastSync) > fiveMinutes
    }
}

/**
 * Result of a sync operation
 */
data class SyncResult(
    val playlistsSynced: Int,
    val songsSynced: Int,
    val errors: List<String>
) {
    val isSuccess: Boolean get() = errors.isEmpty()
    val hasPartialFailure: Boolean get() = errors.isNotEmpty() && (playlistsSynced > 0 || songsSynced > 0)
}
