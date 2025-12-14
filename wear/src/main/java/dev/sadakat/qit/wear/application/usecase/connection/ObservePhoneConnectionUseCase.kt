package dev.sadakat.qit.wear.application.usecase.connection

import dev.sadakat.qit.shared.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to observe phone connection status as a Flow
 */
@Singleton
class ObservePhoneConnectionUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(): Flow<Boolean> {
        return syncRepository.observeWatchConnection()
            .catch { e ->
                // In case of error, emit false to indicate disconnected
                emit(false)
            }
    }
}