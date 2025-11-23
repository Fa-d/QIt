package dev.sadakat.qit.application.usecase.sync

import dev.sadakat.qit.application.usecase.UseCaseWithParams
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.event.PlaylistSyncedToWatch
import dev.sadakat.qit.shared.domain.service.SyncCoordinator
import javax.inject.Inject

/**
 * Use case for syncing a specific playlist to watch
 */
class SyncPlaylistToWatchUseCase @Inject constructor(
    private val syncCoordinator: SyncCoordinator,
    private val eventPublisher: DomainEventPublisher
) : UseCaseWithParams<PlaylistId, Unit>() {

    override suspend fun invoke(params: PlaylistId): Result<Unit> {
        return try {
            val result = syncCoordinator.syncPlaylistToWatch(params)

            result.onSuccess {
                eventPublisher.publish(PlaylistSyncedToWatch(playlistId = params))
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
