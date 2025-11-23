package dev.sadakat.qit.application.usecase.sync

import dev.sadakat.qit.application.usecase.BaseUseCase
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.event.SyncCompleted
import dev.sadakat.qit.shared.domain.event.SyncFailed
import dev.sadakat.qit.shared.domain.event.SyncStarted
import dev.sadakat.qit.shared.domain.event.SyncType
import dev.sadakat.qit.shared.domain.service.SyncCoordinator
import dev.sadakat.qit.shared.domain.service.SyncResult
import javax.inject.Inject

/**
 * Use case for syncing all playlists and songs to watch
 */
class SyncAllToWatchUseCase @Inject constructor(
    private val syncCoordinator: SyncCoordinator,
    private val eventPublisher: DomainEventPublisher
) : BaseUseCase<SyncResult>() {

    override suspend fun invoke(): Result<SyncResult> {
        return try {
            val startTime = System.currentTimeMillis()

            // Publish sync started event
            eventPublisher.publish(SyncStarted(SyncType.FULL_SYNC))

            // Perform sync
            val result = syncCoordinator.syncAllToWatch()

            result.onSuccess { syncResult ->
                val duration = System.currentTimeMillis() - startTime

                if (syncResult.isSuccess) {
                    eventPublisher.publish(
                        SyncCompleted(
                            syncType = SyncType.FULL_SYNC,
                            playlistsSynced = syncResult.playlistsSynced,
                            songsSynced = syncResult.songsSynced,
                            duration = duration
                        )
                    )
                } else {
                    eventPublisher.publish(
                        SyncFailed(
                            syncType = SyncType.FULL_SYNC,
                            error = syncResult.errors.joinToString(", ")
                        )
                    )
                }
            }

            result.onFailure { error ->
                eventPublisher.publish(
                    SyncFailed(
                        syncType = SyncType.FULL_SYNC,
                        error = error.message ?: "Unknown error"
                    )
                )
            }

            result
        } catch (e: Exception) {
            eventPublisher.publish(
                SyncFailed(
                    syncType = SyncType.FULL_SYNC,
                    error = e.message ?: "Unknown error"
                )
            )
            Result.failure(e)
        }
    }
}
