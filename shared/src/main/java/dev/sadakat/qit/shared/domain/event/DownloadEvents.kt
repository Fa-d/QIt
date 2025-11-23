package dev.sadakat.qit.shared.domain.event

import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality

/**
 * Domain events related to download operations
 */

data class DownloadStarted(
    val songId: SongId,
    val quality: AudioQuality
) : DomainEvent

data class DownloadProgressUpdated(
    val songId: SongId,
    val progress: Float
) : DomainEvent

data class DownloadCompleted(
    val songId: SongId,
    val localPath: String,
    val duration: Long // milliseconds
) : DomainEvent

data class DownloadFailed(
    val songId: SongId,
    val error: String
) : DomainEvent

data class DownloadCancelled(
    val songId: SongId
) : DomainEvent

data class DownloadPaused(
    val songId: SongId,
    val progress: Float
) : DomainEvent

data class DownloadResumed(
    val songId: SongId
) : DomainEvent

data class DownloadDeleted(
    val songId: SongId,
    val freedSpace: Long // bytes
) : DomainEvent

data class PlaylistDownloadStarted(
    val playlistId: PlaylistId,
    val songCount: Int
) : DomainEvent

data class PlaylistDownloadCompleted(
    val playlistId: PlaylistId,
    val successCount: Int,
    val failureCount: Int
) : DomainEvent
