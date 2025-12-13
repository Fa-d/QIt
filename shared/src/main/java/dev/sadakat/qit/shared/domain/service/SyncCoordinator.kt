package dev.sadakat.qit.shared.domain.service

import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.valueobject.ConflictResolutionStrategy
import dev.sadakat.qit.shared.domain.valueobject.EntityType
import dev.sadakat.qit.shared.domain.valueobject.SyncConflict

/**
 * Domain Service for coordinating sync operations between phone and watch
 * Handles conflict detection and resolution during bidirectional sync
 */
class SyncCoordinator(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository,
    private val syncRepository: SyncRepository,
    private val conflictResolver: ConflictResolver = ConflictResolver()
) {

    /**
     * Current conflict resolution strategy for automatic resolution
     */
    private var conflictResolutionStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.DEFAULT

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

    /**
     * Sets the conflict resolution strategy for automatic conflict resolution
     * This strategy will be used when conflicts are detected during sync
     *
     * @param strategy The strategy to use (LAST_WRITE_WINS, PHONE_WINS, WATCH_WINS, or MANUAL)
     */
    fun setConflictResolutionStrategy(strategy: ConflictResolutionStrategy) {
        this.conflictResolutionStrategy = strategy
    }

    /**
     * Gets the current conflict resolution strategy
     */
    fun getConflictResolutionStrategy(): ConflictResolutionStrategy {
        return conflictResolutionStrategy
    }

    /**
     * Detects conflicts between phone and watch versions of playlists and songs
     * This method should be called during bidirectional sync to identify conflicts
     *
     * @param phonePlaylists List of playlists from the phone
     * @param watchPlaylists List of playlists from the watch
     * @return List of detected conflicts
     */
    suspend fun detectConflicts(
        phonePlaylists: List<Playlist>,
        watchPlaylists: List<Playlist>
    ): List<SyncConflict> {
        val conflicts = mutableListOf<SyncConflict>()

        // Create maps for efficient lookup
        val phonePlaylistMap = phonePlaylists.associateBy { it.id }
        val watchPlaylistMap = watchPlaylists.associateBy { it.id }

        // Find playlists that exist in both and have conflicts
        for ((playlistId, phonePlaylist) in phonePlaylistMap) {
            val watchPlaylist = watchPlaylistMap[playlistId] ?: continue

            // Check if there's a conflict
            if (conflictResolver.hasPlaylistConflict(phonePlaylist, watchPlaylist)) {
                val conflict = SyncConflict.create(
                    entityId = playlistId.value,
                    entityType = EntityType.PLAYLIST,
                    phoneVersion = phonePlaylist,
                    watchVersion = watchPlaylist,
                    phoneTimestamp = phonePlaylist.updatedAt,
                    watchTimestamp = watchPlaylist.updatedAt,
                    strategy = conflictResolutionStrategy
                )
                conflicts.add(conflict)
            }
        }

        return conflicts
    }

    /**
     * Detects all conflicts without requiring explicit lists
     * Fetches data from repositories to detect conflicts
     *
     * Note: This is a simplified version. In a real implementation,
     * you would need a way to fetch watch-side playlists, which would
     * typically be done through the SyncRepository
     */
    suspend fun detectConflicts(): List<SyncConflict> {
        // This is a placeholder for now since we need watch-side data
        // In a real implementation, you would:
        // 1. Fetch phone playlists from playlistRepository
        // 2. Fetch watch playlists from syncRepository or a watch-specific repository
        // 3. Compare them using detectConflicts(phonePlaylists, watchPlaylists)

        val conflicts = mutableListOf<SyncConflict>()

        // For now, return empty list
        // Implementation would require watch-side data access
        return conflicts
    }

    /**
     * Resolves detected conflicts and applies the resolved versions
     * Returns the result of conflict resolution including any unresolved conflicts
     *
     * @param conflicts List of conflicts to resolve
     * @return ConflictResolutionResult containing resolved and unresolved conflicts
     */
    suspend fun resolveAndApplyConflicts(
        conflicts: List<SyncConflict>
    ): ConflictResolutionResult {
        if (conflicts.isEmpty()) {
            return ConflictResolutionResult(
                resolvedConflicts = emptyList(),
                unresolvedConflicts = emptyList(),
                appliedCount = 0,
                failedCount = 0
            )
        }

        // Resolve conflicts
        val (resolved, unresolved) = conflictResolver.resolveConflicts(conflicts)

        var appliedCount = 0
        var failedCount = 0

        // Apply resolved conflicts
        for (conflict in resolved) {
            try {
                when (conflict.entityType) {
                    EntityType.PLAYLIST -> {
                        val playlist = conflict.resolvedVersion as? Playlist
                        if (playlist != null) {
                            // Update both phone and watch with resolved version
                            playlistRepository.savePlaylist(playlist)
                            syncRepository.syncPlaylistToWatch(playlist)
                            appliedCount++
                        } else {
                            failedCount++
                        }
                    }

                    EntityType.SONG, EntityType.SONG_METADATA -> {
                        val song = conflict.resolvedVersion as? Song
                        if (song != null) {
                            // Update both phone and watch with resolved version
                            musicRepository.saveSong(song)
                            syncRepository.syncSongToWatch(song)
                            appliedCount++
                        } else {
                            failedCount++
                        }
                    }
                }
            } catch (e: Exception) {
                failedCount++
            }
        }

        return ConflictResolutionResult(
            resolvedConflicts = resolved,
            unresolvedConflicts = unresolved,
            appliedCount = appliedCount,
            failedCount = failedCount
        )
    }

    /**
     * Performs sync with conflict detection and resolution
     * This is an enhanced version of syncAllToWatch that handles conflicts
     *
     * @param phonePlaylists Playlists from phone
     * @param watchPlaylists Playlists from watch (if available)
     * @return Enhanced sync result with conflict information
     */
    suspend fun syncWithConflictResolution(
        phonePlaylists: List<Playlist>,
        watchPlaylists: List<Playlist>
    ): Result<EnhancedSyncResult> {
        return try {
            if (!syncRepository.isWatchConnected()) {
                return Result.failure(IllegalStateException("Watch not connected"))
            }

            // Detect conflicts
            val conflicts = detectConflicts(phonePlaylists, watchPlaylists)

            // Resolve and apply conflicts
            val resolutionResult = resolveAndApplyConflicts(conflicts)

            // Continue with normal sync for non-conflicting items
            val syncResult = syncAllToWatch().getOrThrow()

            Result.success(
                EnhancedSyncResult(
                    playlistsSynced = syncResult.playlistsSynced,
                    songsSynced = syncResult.songsSynced,
                    errors = syncResult.errors,
                    conflictsDetected = conflicts.size,
                    conflictsResolved = resolutionResult.appliedCount,
                    unresolvedConflicts = resolutionResult.unresolvedConflicts
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
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

/**
 * Enhanced sync result that includes conflict resolution information
 */
data class EnhancedSyncResult(
    val playlistsSynced: Int,
    val songsSynced: Int,
    val errors: List<String>,
    val conflictsDetected: Int,
    val conflictsResolved: Int,
    val unresolvedConflicts: List<SyncConflict>
) {
    val isSuccess: Boolean get() = errors.isEmpty() && unresolvedConflicts.isEmpty()
    val hasConflicts: Boolean get() = conflictsDetected > 0
    val hasUnresolvedConflicts: Boolean get() = unresolvedConflicts.isNotEmpty()
    val hasPartialFailure: Boolean get() = errors.isNotEmpty() && (playlistsSynced > 0 || songsSynced > 0)

    /**
     * Converts to basic SyncResult
     */
    fun toSyncResult(): SyncResult {
        return SyncResult(
            playlistsSynced = playlistsSynced,
            songsSynced = songsSynced,
            errors = errors
        )
    }
}

/**
 * Result of conflict resolution operation
 */
data class ConflictResolutionResult(
    val resolvedConflicts: List<SyncConflict>,
    val unresolvedConflicts: List<SyncConflict>,
    val appliedCount: Int,
    val failedCount: Int
) {
    val totalConflicts: Int get() = resolvedConflicts.size + unresolvedConflicts.size
    val resolutionRate: Float get() = if (totalConflicts > 0) {
        resolvedConflicts.size.toFloat() / totalConflicts.toFloat()
    } else {
        1.0f
    }
    val isFullyResolved: Boolean get() = unresolvedConflicts.isEmpty() && failedCount == 0
}
