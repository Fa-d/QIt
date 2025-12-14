package dev.sadakat.qit.application.usecase.connection

import dev.sadakat.qit.shared.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveWatchConnectionUseCase @Inject constructor(
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