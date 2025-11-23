package dev.sadakat.qit.wear.application.usecase.download

import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.event.DownloadStarted
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.repository.SettingsRepository
import dev.sadakat.qit.wear.application.usecase.UseCaseWithParams
import javax.inject.Inject

/**
 * Use case for downloading a song to watch storage
 */
class DownloadSongUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val settingsRepository: SettingsRepository,
    private val eventPublisher: DomainEventPublisher
) : UseCaseWithParams<SongId, Unit>() {

    override suspend fun invoke(params: SongId): Result<Unit> {
        return try {
            // Get preferred download quality
            val quality = settingsRepository.getDownloadQuality()

            // Publish event
            eventPublisher.publish(DownloadStarted(songId = params, quality = quality))

            // Start download
            downloadRepository.downloadSong(params, quality)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
