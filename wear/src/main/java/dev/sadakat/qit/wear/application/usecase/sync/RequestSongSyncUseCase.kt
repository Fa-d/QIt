package dev.sadakat.qit.wear.application.usecase.sync

import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to request song synchronization from phone
 */
@Singleton
class RequestSongSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(playlistId: PlaylistId? = null): Result<Unit> {
        return try {
            if (playlistId != null) {
                syncRepository.requestSongSyncFromPhone(playlistId)
            } else {
                // Request all songs sync
                syncRepository.requestSongSyncFromPhone(PlaylistId(""))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}