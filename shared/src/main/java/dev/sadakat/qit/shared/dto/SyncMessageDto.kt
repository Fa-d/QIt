package dev.sadakat.qit.shared.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for sync messages between phone and watch
 */

@Serializable
data class PlaylistSyncMessage(
    val playlists: List<PlaylistDto>,
    /**
     * True when this message is part of a FULL sync session (all data).
     * Additive optional field: old senders/receivers that don't know it stay
     * compatible via ignoreUnknownKeys / default value.
     */
    val fullSync: Boolean = false
)

@Serializable
data class SongSyncMessage(
    val songs: List<SongDto>,
    /** True when this message is part of a FULL sync session (all data). */
    val fullSync: Boolean = false
)

@Serializable
data class DownloadRequestMessage(
    val songId: String,
    val quality: String // LOW, MEDIUM, HIGH, ORIGINAL
)

/**
 * Sent by the phone when a download transfer is about to begin
 */
@Serializable
data class DownloadStartMessage(
    val songId: String,
    val quality: String,
    val fileSize: Long
)

/**
 * Sent by the phone during a download transfer to report progress
 */
@Serializable
data class DownloadProgressMessage(
    val songId: String,
    val progress: Float
)

/**
 * Sent by the phone when a download transfer finished (successfully or not)
 */
@Serializable
data class DownloadCompleteMessage(
    val songId: String,
    val success: Boolean
)

@Serializable
data class StreamRequestMessage(
    val songId: String,
    val quality: String
)

/**
 * Sent by the phone when an audio-stream request FAILS before a channel could
 * be opened (song not found, file missing, ...) so the watch can surface an
 * error instead of buffering until timeout.
 */
@Serializable
data class StreamErrorMessage(
    val songId: String,
    val reason: String = ""
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
