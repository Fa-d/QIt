package dev.sadakat.qit.wear.application.usecase.download

import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.event.DownloadCancelled
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.wear.application.usecase.UseCaseWithParams
import javax.inject.Inject

/**
 * Use case for cancelling a song download
 */
class CancelDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val eventPublisher: DomainEventPublisher
) : UseCaseWithParams<SongId, Unit>() {

    override suspend fun invoke(params: SongId): Result<Unit> {
        return try {
            val result = downloadRepository.cancelDownload(params)

            result.onSuccess {
                eventPublisher.publish(DownloadCancelled(songId = params))
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
