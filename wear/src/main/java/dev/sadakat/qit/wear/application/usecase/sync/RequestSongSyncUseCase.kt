package dev.sadakat.qit.wear.application.usecase.sync

import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to request song synchronization from phone.
 *
 * A null playlistId requests ALL songs from the phone.
 * (Note: PlaylistId rejects blank values, so "all" must be expressed with null.)
 */
@Singleton
class RequestSongSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(playlistId: PlaylistId? = null): Result<Unit> {
        return try {
            syncRepository.requestSongSyncFromPhone(playlistId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
