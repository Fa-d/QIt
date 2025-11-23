package dev.sadakat.qit.shared.domain.event

import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.SongId

/**
 * Domain events related to Playlist operations
 */

data class PlaylistCreated(
    val playlistId: PlaylistId,
    val playlistName: String
) : DomainEvent

data class PlaylistUpdated(
    val playlistId: PlaylistId,
    val playlistName: String
) : DomainEvent

data class PlaylistDeleted(
    val playlistId: PlaylistId
) : DomainEvent

data class SongAddedToPlaylist(
    val playlistId: PlaylistId,
    val songId: SongId
) : DomainEvent

data class SongRemovedFromPlaylist(
    val playlistId: PlaylistId,
    val songId: SongId
) : DomainEvent

data class PlaylistReordered(
    val playlistId: PlaylistId,
    val songId: SongId,
    val fromIndex: Int,
    val toIndex: Int
) : DomainEvent

data class PlaylistCleared(
    val playlistId: PlaylistId
) : DomainEvent
