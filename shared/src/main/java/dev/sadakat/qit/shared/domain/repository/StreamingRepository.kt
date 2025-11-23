package dev.sadakat.qit.shared.domain.repository

import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for audio streaming operations
 */
interface StreamingRepository {

    /**
     * Streams audio to watch in real-time
     */
    suspend fun streamAudioToWatch(
        songId: SongId,
        quality: AudioQuality = AudioQuality.ORIGINAL
    ): Result<Unit>

    /**
     * Requests audio stream from phone (watch-side)
     */
    suspend fun requestStreamFromPhone(
        songId: SongId,
        quality: AudioQuality = AudioQuality.MEDIUM
    ): Result<Unit>

    /**
     * Stops active streaming session
     */
    suspend fun stopStreaming(songId: SongId): Result<Unit>

    /**
     * Observes streaming status
     */
    fun observeStreamingStatus(): Flow<StreamingStatus>

    /**
     * Gets available streaming quality based on connection
     */
    suspend fun getRecommendedQuality(): AudioQuality
}

/**
 * Represents the status of audio streaming
 */
sealed class StreamingStatus {
    object Idle : StreamingStatus()
    data class Streaming(val songId: SongId, val quality: AudioQuality) : StreamingStatus()
    data class Buffering(val songId: SongId, val progress: Float) : StreamingStatus()
    data class Error(val message: String) : StreamingStatus()
}
