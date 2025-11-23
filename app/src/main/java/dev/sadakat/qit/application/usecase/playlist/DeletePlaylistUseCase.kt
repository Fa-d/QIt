package dev.sadakat.qit.application.usecase.playlist

import dev.sadakat.qit.application.usecase.UseCaseWithParams
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.event.PlaylistDeleted
import dev.sadakat.qit.shared.domain.service.PlaylistOrchestrator
import javax.inject.Inject

/**
 * Use case for deleting a playlist
 */
class DeletePlaylistUseCase @Inject constructor(
    private val playlistOrchestrator: PlaylistOrchestrator,
    private val eventPublisher: DomainEventPublisher
) : UseCaseWithParams<PlaylistId, Unit>() {

    override suspend fun invoke(params: PlaylistId): Result<Unit> {
        return try {
            val result = playlistOrchestrator.deletePlaylist(params)

            result.onSuccess {
                eventPublisher.publish(PlaylistDeleted(playlistId = params))
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
