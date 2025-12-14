package dev.sadakat.qit.wear.application.usecase.playback

import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to send playback commands to phone
 */
@Singleton
class SendPlaybackCommandUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(
        command: String,
        songId: SongId? = null
    ): Result<Unit> {
        return try {
            syncRepository.sendPlaybackCommand(command, songId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}