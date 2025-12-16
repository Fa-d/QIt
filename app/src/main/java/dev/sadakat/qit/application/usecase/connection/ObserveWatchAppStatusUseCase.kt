package dev.sadakat.qit.application.usecase.connection

import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.valueobject.WatchAppStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveWatchAppStatusUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(): Flow<WatchAppStatus> {
        return syncRepository.observeWatchAppStatus()
            .catch { e ->
                // In case of error, emit not installed status
                emit(WatchAppStatus.notInstalled())
            }
    }
}
