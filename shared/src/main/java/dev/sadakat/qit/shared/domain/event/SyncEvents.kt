package dev.sadakat.qit.shared.domain.event

import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.SongId

/**
 * Domain events related to Phone-Watch synchronization
 */

data class SyncStarted(
    val syncType: SyncType
) : DomainEvent

data class SyncCompleted(
    val syncType: SyncType,
    val playlistsSynced: Int,
    val songsSynced: Int,
    val duration: Long // milliseconds
) : DomainEvent

data class SyncFailed(
    val syncType: SyncType,
    val error: String
) : DomainEvent

data class PlaylistSyncedToWatch(
    val playlistId: PlaylistId
) : DomainEvent

data class SongSyncedToWatch(
    val songId: SongId
) : DomainEvent

data class WatchConnected(
    val nodeId: String
) : DomainEvent

data class WatchDisconnected(
    val nodeId: String
) : DomainEvent

enum class SyncType {
    FULL_SYNC,
    DELTA_SYNC,
    SINGLE_PLAYLIST,
    SINGLE_SONG
}
