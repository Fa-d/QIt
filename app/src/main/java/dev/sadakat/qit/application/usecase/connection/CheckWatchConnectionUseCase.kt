package dev.sadakat.qit.application.usecase.connection

import dev.sadakat.qit.shared.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckWatchConnectionUseCase @Inject constructor(
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