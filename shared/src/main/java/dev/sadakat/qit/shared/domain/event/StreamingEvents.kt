package dev.sadakat.qit.shared.domain.event

import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.StreamingMode

/**
 * Domain events related to audio streaming
 */

data class StreamStarted(
    val songId: SongId,
    val quality: AudioQuality,
    val mode: StreamingMode
) : DomainEvent

data class StreamBuffering(
    val songId: SongId,
    val progress: Float
) : DomainEvent

data class StreamReady(
    val songId: SongId
) : DomainEvent

data class StreamEnded(
    val songId: SongId
) : DomainEvent

data class StreamFailed(
    val songId: SongId,
    val error: String
) : DomainEvent

data class StreamingStopped(
    val songId: SongId,
    val reason: String
) : DomainEvent

data class StreamingQualityChanged(
    val songId: SongId,
    val fromQuality: AudioQuality,
    val toQuality: AudioQuality,
    val reason: String
) : DomainEvent

data class StreamingModeChanged(
    val songId: SongId,
    val fromMode: StreamingMode,
    val toMode: StreamingMode,
    val reason: String
) : DomainEvent
