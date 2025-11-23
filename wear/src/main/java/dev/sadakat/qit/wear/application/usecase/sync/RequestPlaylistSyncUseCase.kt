package dev.sadakat.qit.wear.application.usecase.sync

import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.event.SyncStarted
import dev.sadakat.qit.shared.domain.event.SyncType
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.wear.application.usecase.BaseUseCase
import javax.inject.Inject

/**
 * Use case for requesting playlist sync from phone
 */
class RequestPlaylistSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val eventPublisher: DomainEventPublisher
) : BaseUseCase<Unit>() {

    override suspend fun invoke(): Result<Unit> {
        return try {
            eventPublisher.publish(SyncStarted(SyncType.FULL_SYNC))
            syncRepository.requestPlaylistSyncFromPhone()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
