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

            // Initiate streaming if needed
            val initiateResult = streamingCoordinator.initiateStreaming(params.songId, strategy)
            if (initiateResult.isFailure) {
                return Result.failure(initiateResult.exceptionOrNull()!!)
            }

            // Publish playback started event
            eventPublisher.publish(
                PlaybackStarted(
                    songId = params.songId,
                    playlistId = params.playlistId
                )
            )

            // Return playback source
            val playbackSource = when {
                song.isAvailableOnWatch() -> PlaybackSource.Local(song)
                else -> PlaybackSource.Streaming(song, strategy)
            }

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
