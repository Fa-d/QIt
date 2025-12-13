package dev.sadakat.qit.shared.domain.service

import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.ConflictResolutionStrategy
import dev.sadakat.qit.shared.domain.valueobject.SyncConflict

/**
 * Domain Service for resolving synchronization conflicts between phone and watch
 * Applies different resolution strategies to conflicting entity versions
 */
class ConflictResolver {

    /**
     * Resolves a conflict using its configured strategy
     * Returns the conflict with resolvedVersion set
     *
     * @throws IllegalStateException if strategy is MANUAL (requires user intervention)
     * @throws IllegalArgumentException if entity types don't match expected types
     */
    fun resolveConflict(conflict: SyncConflict): SyncConflict {
        if (conflict.strategy.requiresUserIntervention()) {
            throw IllegalStateException(
                "Cannot auto-resolve conflict with MANUAL strategy. User intervention required."
            )
        }

        if (conflict.isResolved()) {
            return conflict
        }

        val resolved = when (conflict.strategy) {
            ConflictResolutionStrategy.LAST_WRITE_WINS -> {
                if (conflict.isPhoneNewer()) {
                    conflict.phoneVersion
                } else if (conflict.isWatchNewer()) {
                    conflict.watchVersion
                } else {
                    // Same timestamp - prefer phone version as tie-breaker
                    conflict.phoneVersion
                }
            }

            ConflictResolutionStrategy.PHONE_WINS -> {
                conflict.phoneVersion
            }

            ConflictResolutionStrategy.WATCH_WINS -> {
                conflict.watchVersion
            }

            ConflictResolutionStrategy.MANUAL -> {
                // This should never be reached due to the check above
                throw IllegalStateException("MANUAL strategy should not reach resolution")
            }
        }

        return conflict.withResolvedVersion(resolved ?: conflict.phoneVersion!!)
    }

    /**
     * Resolves a conflict between two Playlist versions
     * Applies the specified strategy and returns the winning playlist
     *
     * @param phone Playlist version from phone
     * @param watch Playlist version from watch
     * @param strategy Resolution strategy to apply
     * @return The resolved Playlist based on the strategy
     * @throws IllegalStateException if strategy is MANUAL
     */
    fun resolvePlaylistConflict(
        phone: Playlist,
        watch: Playlist,
        strategy: ConflictResolutionStrategy = ConflictResolutionStrategy.DEFAULT
    ): Playlist {
        require(phone.id == watch.id) {
            "Cannot resolve conflict between playlists with different IDs: ${phone.id} vs ${watch.id}"
        }

        if (strategy.requiresUserIntervention()) {
            throw IllegalStateException(
                "Cannot auto-resolve playlist conflict with MANUAL strategy. User intervention required."
            )
        }

        return when (strategy) {
            ConflictResolutionStrategy.LAST_WRITE_WINS -> {
                if (phone.updatedAt > watch.updatedAt) {
                    phone
                } else if (watch.updatedAt > phone.updatedAt) {
                    watch
                } else {
                    // Same timestamp - merge with phone metadata and watch songs if watch has more
                    mergePlaylistsOnTie(phone, watch)
                }
            }

            ConflictResolutionStrategy.PHONE_WINS -> phone

            ConflictResolutionStrategy.WATCH_WINS -> watch

            ConflictResolutionStrategy.MANUAL -> {
                throw IllegalStateException("MANUAL strategy should not reach resolution")
            }
        }
    }

    /**
     * Resolves a conflict between two Song metadata versions
     * This only resolves metadata conflicts, not download status
     *
     * @param phone Song version from phone
     * @param watch Song version from watch
     * @param strategy Resolution strategy to apply
     * @return The resolved Song based on the strategy
     * @throws IllegalStateException if strategy is MANUAL
     */
    fun resolveSongMetadataConflict(
        phone: Song,
        watch: Song,
        strategy: ConflictResolutionStrategy = ConflictResolutionStrategy.DEFAULT
    ): Song {
        require(phone.id == watch.id) {
            "Cannot resolve conflict between songs with different IDs: ${phone.id} vs ${watch.id}"
        }

        if (strategy.requiresUserIntervention()) {
            throw IllegalStateException(
                "Cannot auto-resolve song conflict with MANUAL strategy. User intervention required."
            )
        }

        // Compare based on dateAdded timestamp for songs
        return when (strategy) {
            ConflictResolutionStrategy.LAST_WRITE_WINS -> {
                if (phone.dateAdded > watch.dateAdded) {
                    phone
                } else if (watch.dateAdded > phone.dateAdded) {
                    watch
                } else {
                    // Same timestamp - prefer phone version
                    phone
                }
            }

            ConflictResolutionStrategy.PHONE_WINS -> phone

            ConflictResolutionStrategy.WATCH_WINS -> watch

            ConflictResolutionStrategy.MANUAL -> {
                throw IllegalStateException("MANUAL strategy should not reach resolution")
            }
        }
    }

    /**
     * Resolves multiple conflicts in batch
     * Returns list of resolved conflicts
     * Skips conflicts that require manual intervention
     *
     * @param conflicts List of conflicts to resolve
     * @return Pair of (resolved conflicts, unresolved conflicts)
     */
    fun resolveConflicts(conflicts: List<SyncConflict>): Pair<List<SyncConflict>, List<SyncConflict>> {
        val resolved = mutableListOf<SyncConflict>()
        val unresolved = mutableListOf<SyncConflict>()

        for (conflict in conflicts) {
            if (conflict.strategy.requiresUserIntervention()) {
                unresolved.add(conflict)
            } else {
                try {
                    resolved.add(resolveConflict(conflict))
                } catch (e: Exception) {
                    // If resolution fails, mark as unresolved
                    unresolved.add(conflict)
                }
            }
        }

        return Pair(resolved, unresolved)
    }

    /**
     * Merges two playlists when they have the same timestamp
     * Strategy: Use phone metadata but include union of songs from both
     */
    private fun mergePlaylistsOnTie(phone: Playlist, watch: Playlist): Playlist {
        // Start with phone version
        val phoneSongs = phone.getSongIds().toSet()
        val watchSongs = watch.getSongIds().toSet()

        // Union of songs from both, maintaining phone's order first, then watch's unique songs
        val mergedSongIds = mutableListOf<SongId>()
        mergedSongIds.addAll(phone.getSongIds())

        // Add watch songs that aren't in phone
        val uniqueWatchSongs = watchSongs - phoneSongs
        mergedSongIds.addAll(watch.getSongIds().filter { it in uniqueWatchSongs })

        // Create merged playlist (will update timestamp to current time)
        return phone.copy(
            songIds = mergedSongIds,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Detects if two playlists have a conflict
     * Conflict exists if they have different content but same ID
     */
    fun hasPlaylistConflict(phone: Playlist, watch: Playlist): Boolean {
        if (phone.id != watch.id) return false

        // Check if metadata or song lists differ
        return phone.name != watch.name ||
                phone.description != watch.description ||
                phone.getSongIds() != watch.getSongIds() ||
                phone.coverArtUri != watch.coverArtUri
    }

    /**
     * Detects if two songs have a metadata conflict
     * Conflict exists if they have different metadata but same ID
     */
    fun hasSongMetadataConflict(phone: Song, watch: Song): Boolean {
        if (phone.id != watch.id) return false

        return !phone.hasSameMetadata(watch)
    }
}
