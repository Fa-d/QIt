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
 * Use case for performing delta sync (only changes since last sync)
 */
class PerformDeltaSyncUseCase @Inject constructor(
    private val syncCoordinator: SyncCoordinator,
    private val eventPublisher: DomainEventPublisher
) : BaseUseCase<SyncResult>() {

    override suspend fun invoke(): Result<SyncResult> {
        return try {
            val startTime = System.currentTimeMillis()

            eventPublisher.publish(SyncStarted(SyncType.DELTA_SYNC))

            val result = syncCoordinator.performDeltaSync()

            result.onSuccess { syncResult ->
                val duration = System.currentTimeMillis() - startTime

                if (syncResult.isSuccess) {
                    eventPublisher.publish(
                        SyncCompleted(
                            syncType = SyncType.DELTA_SYNC,
                            playlistsSynced = syncResult.playlistsSynced,
                            songsSynced = syncResult.songsSynced,
                            duration = duration
                        )
                    )
                }
            }

            result.onFailure { error ->
                eventPublisher.publish(
                    SyncFailed(
                        syncType = SyncType.DELTA_SYNC,
                        error = error.message ?: "Unknown error"
                    )
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
