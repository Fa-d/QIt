package dev.sadakat.qit.shared.domain.service

import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.SettingsRepository
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.StreamingMode

/**
 * Domain Service for coordinating streaming operations
 * Decides between real-time streaming vs progressive download
 */
class StreamingCoordinator(
    private val streamingRepository: StreamingRepository,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository
) {

    /**
     * Determines the best streaming strategy for a song
     */
    suspend fun determineStreamingStrategy(song: Song): StreamingStrategy {
        // If song is already downloaded, play locally
        if (song.isAvailableOnWatch()) {
            return StreamingStrategy.Local(song.downloadStatus.toString())
        }

        // Check if phone is connected
        if (!syncRepository.isWatchConnected()) {
            return StreamingStrategy.Unavailable("Phone not connected")
        }

        // Get user preference
        val preferredMode = settingsRepository.getStreamingMode()

        // Get recommended quality based on connection
        val recommendedQuality = streamingRepository.getRecommendedQuality()
        val userPreferredQuality = settingsRepository.getStreamingQuality()

        // Use lower of recommended and preferred quality
        val quality = if (recommendedQuality.bitrate < userPreferredQuality.bitrate) {
            recommendedQuality
        } else {
            userPreferredQuality
        }

        return when (preferredMode) {
            StreamingMode.LOCAL -> StreamingStrategy.Unavailable("Song not downloaded")
            StreamingMode.REAL_TIME -> StreamingStrategy.RealTime(quality)
            StreamingMode.PROGRESSIVE -> StreamingStrategy.Progressive(quality)
        }
    }

    /**
     * Initiates streaming based on strategy
     */
    suspend fun initiateStreaming(
        songId: SongId,
        strategy: StreamingStrategy
    ): Result<Unit> {
        return when (strategy) {
            is StreamingStrategy.Local -> {
                // No streaming needed - song is local
                Result.success(Unit)
            }
            is StreamingStrategy.RealTime -> {
                streamingRepository.requestStreamFromPhone(songId, strategy.quality)
            }
            is StreamingStrategy.Progressive -> {
                streamingRepository.requestStreamFromPhone(songId, strategy.quality)
            }
            is StreamingStrategy.Unavailable -> {
                Result.failure(IllegalStateException(strategy.reason))
            }
        }
    }

    /**
     * Handles connection loss during streaming
     * Attempts to switch to alternative strategy
     */
    suspend fun handleConnectionLoss(
        songId: SongId,
        currentStrategy: StreamingStrategy
    ): Result<StreamingStrategy> {
        return try {
            when (currentStrategy) {
                is StreamingStrategy.RealTime -> {
                    // Try to switch to progressive download
                    Result.success(
                        StreamingStrategy.Progressive(currentStrategy.quality)
                    )
                }
                is StreamingStrategy.Progressive -> {
                    // Already progressive, can't fallback further
                    Result.success(
                        StreamingStrategy.Unavailable("Connection lost during download")
                    )
                }
                else -> Result.success(currentStrategy)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stops any active streaming for a song
     */
    suspend fun stopStreaming(songId: SongId): Result<Unit> {
        return streamingRepository.stopStreaming(songId)
    }
}

/**
 * Represents different streaming strategies
 */
sealed class StreamingStrategy {
    /**
     * Play from local storage
     */
    data class Local(val path: String) : StreamingStrategy()

    /**
     * Real-time streaming without local storage
     */
    data class RealTime(val quality: AudioQuality) : StreamingStrategy()

    /**
     * Progressive download while playing
     */
    data class Progressive(val quality: AudioQuality) : StreamingStrategy()

    /**
     * Streaming not available
     */
    data class Unavailable(val reason: String) : StreamingStrategy()
}
