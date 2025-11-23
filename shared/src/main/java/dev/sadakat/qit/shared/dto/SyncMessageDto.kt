package dev.sadakat.qit.shared.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for sync messages between phone and watch
 */

@Serializable
data class PlaylistSyncMessage(
    val playlists: List<PlaylistDto>
)

@Serializable
data class SongSyncMessage(
    val songs: List<SongDto>
)

@Serializable
data class DownloadRequestMessage(
    val songId: String,
    val quality: String // LOW, MEDIUM, HIGH, ORIGINAL
)

@Serializable
data class StreamRequestMessage(
    val songId: String,
    val quality: String
)

@Serializable
data class PlaybackCommandMessage(
    val command: String, // PLAY, PAUSE, STOP, SKIP_NEXT, SKIP_PREVIOUS
    val songId: String? = null,
    val position: Long? = null
)

@Serializable
data class SyncStatusMessage(
    val lastSyncTimestamp: Long,
    val playlistCount: Int,
    val songCount: Int
)
