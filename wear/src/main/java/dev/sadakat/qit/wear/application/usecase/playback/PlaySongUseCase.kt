package dev.sadakat.qit.wear.application.usecase.playback

import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.event.PlaybackStarted
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.service.StreamingCoordinator
import dev.sadakat.qit.wear.application.usecase.UseCaseWithParams
import javax.inject.Inject

/**
 * Use case for playing a song
 * Determines whether to play locally or stream from phone
 */
class PlaySongUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val streamingCoordinator: StreamingCoordinator,
    private val eventPublisher: DomainEventPublisher
) : UseCaseWithParams<PlaySongUseCase.Params, PlaybackSource>() {

    override suspend fun invoke(params: Params): Result<PlaybackSource> {
        return try {
            // Get song
            val songResult = musicRepository.getSongById(params.songId)
            val song = songResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Song not found"))

            // Determine streaming strategy
            val strategy = streamingCoordinator.determineStreamingStrategy(song)

            // Return playback source based on strategy
            val playbackSource = when (strategy) {
                is dev.sadakat.qit.shared.domain.service.StreamingStrategy.Local -> {
                    // Song is available locally, no streaming needed
                    PlaybackSource.Local(song)
                }
                is dev.sadakat.qit.shared.domain.service.StreamingStrategy.RealTime,
                is dev.sadakat.qit.shared.domain.service.StreamingStrategy.Progressive -> {
                    // Need to stream from phone
                    // Initiate streaming request
                    val initiateResult = streamingCoordinator.initiateStreaming(params.songId, strategy)
                    if (initiateResult.isFailure) {
                        return Result.failure(
                            initiateResult.exceptionOrNull() ?: IllegalStateException("Failed to initiate streaming")
                        )
                    }
                    PlaybackSource.Streaming(song, strategy)
                }
                is dev.sadakat.qit.shared.domain.service.StreamingStrategy.Unavailable -> {
                    // Streaming not available
                    return Result.failure(IllegalStateException(strategy.reason))
                }
            }

            // Publish playback started event
            eventPublisher.publish(
                PlaybackStarted(
                    songId = params.songId,
                    playlistId = params.playlistId
                )
            )

            Result.success(playbackSource)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class Params(
        val songId: SongId,
        val playlistId: dev.sadakat.qit.shared.domain.entity.PlaylistId? = null
    )
}

/**
 * Represents the source of playback
 */
sealed class PlaybackSource {
    data class Local(val song: Song) : PlaybackSource()
    data class Streaming(
        val song: Song,
        val strategy: dev.sadakat.qit.shared.domain.service.StreamingStrategy
    ) : PlaybackSource()
}
