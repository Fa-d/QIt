package dev.sadakat.qit.application.usecase.connection

import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.valueobject.WatchAppStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetWatchAppStatusUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<WatchAppStatus> {
        return syncRepository.getWatchAppStatus()
    }
}
