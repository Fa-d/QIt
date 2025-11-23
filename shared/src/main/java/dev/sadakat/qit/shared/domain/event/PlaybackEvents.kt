package dev.sadakat.qit.shared.domain.event

import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.SongId

/**
 * Domain events related to audio playback
 */

data class PlaybackStarted(
    val songId: SongId,
    val playlistId: PlaylistId?
) : DomainEvent

data class PlaybackPaused(
    val songId: SongId,
    val position: Long // milliseconds
) : DomainEvent

data class PlaybackResumed(
    val songId: SongId,
    val position: Long // milliseconds
) : DomainEvent

data class PlaybackStopped(
    val songId: SongId
) : DomainEvent

data class PlaybackCompleted(
    val songId: SongId
) : DomainEvent

data class PlaybackSkippedNext(
    val fromSongId: SongId,
    val toSongId: SongId
) : DomainEvent

data class PlaybackSkippedPrevious(
    val fromSongId: SongId,
    val toSongId: SongId
) : DomainEvent

data class PlaybackSeeked(
    val songId: SongId,
    val fromPosition: Long,
    val toPosition: Long
) : DomainEvent

data class PlaybackError(
    val songId: SongId,
    val error: String
) : DomainEvent

data class PlaybackQueueChanged(
    val queueSize: Int,
    val currentIndex: Int
) : DomainEvent
