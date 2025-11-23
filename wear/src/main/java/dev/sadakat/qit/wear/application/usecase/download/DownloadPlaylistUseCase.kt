package dev.sadakat.qit.wear.application.usecase.download

import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.event.PlaylistDownloadStarted
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.repository.SettingsRepository
import dev.sadakat.qit.wear.application.usecase.UseCaseWithParams
import javax.inject.Inject

/**
 * Use case for downloading an entire playlist to watch storage
 */
class DownloadPlaylistUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    private val eventPublisher: DomainEventPublisher
) : UseCaseWithParams<PlaylistId, Unit>() {

    override suspend fun invoke(params: PlaylistId): Result<Unit> {
        return try {
            // Get songs in playlist
            val songsResult = playlistRepository.getSongsForPlaylist(params)
            val songs = songsResult.getOrNull() ?: return Result.failure(
                IllegalStateException("Failed to get playlist songs")
            )

            // Publish event
            eventPublisher.publish(
                PlaylistDownloadStarted(
                    playlistId = params,
                    songCount = songs.size
                )
            )

            // Get preferred quality
            val quality = settingsRepository.getDownloadQuality()

            // Download playlist
            downloadRepository.downloadPlaylist(params, quality)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
