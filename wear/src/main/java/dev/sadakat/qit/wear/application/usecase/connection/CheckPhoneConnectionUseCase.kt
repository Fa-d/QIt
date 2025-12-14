package dev.sadakat.qit.wear.application.usecase.connection

import dev.sadakat.qit.shared.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to check if the phone is connected
 */
@Singleton
class CheckPhoneConnectionUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        return try {
            val isConnected = syncRepository.isWatchConnected()
            Result.success(isConnected)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}