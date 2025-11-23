package dev.sadakat.qit.application.usecase.playlist

import dev.sadakat.qit.application.usecase.UseCaseWithParams
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.event.PlaylistCreated
import dev.sadakat.qit.shared.domain.service.PlaylistOrchestrator
import javax.inject.Inject

/**
 * Use case for creating a new playlist
 */
class CreatePlaylistUseCase @Inject constructor(
    private val playlistOrchestrator: PlaylistOrchestrator,
    private val eventPublisher: DomainEventPublisher
) : UseCaseWithParams<CreatePlaylistUseCase.Params, Playlist>() {

    override suspend fun invoke(params: Params): Result<Playlist> {
        return try {
            val result = playlistOrchestrator.createPlaylist(
                name = params.name,
                description = params.description,
                coverArtUri = params.coverArtUri
            )

            result.onSuccess { playlist ->
                // Publish domain event
                eventPublisher.publish(
                    PlaylistCreated(
                        playlistId = playlist.id,
                        playlistName = playlist.name
                    )
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class Params(
        val name: String,
        val description: String? = null,
        val coverArtUri: String? = null
    )
}
