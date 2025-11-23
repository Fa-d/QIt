package dev.sadakat.qit.application.usecase.music

import dev.sadakat.qit.application.usecase.BaseUseCase
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import javax.inject.Inject

/**
 * Use case for scanning the device's music library
 */
class ScanMusicLibraryUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val eventPublisher: DomainEventPublisher
) : BaseUseCase<List<Song>>() {

    override suspend fun invoke(): Result<List<Song>> {
        return try {
            // Scan music library
            val result = musicRepository.scanMusicLibrary()

            result.onSuccess { songs ->
                // Save all scanned songs to database
                musicRepository.saveSongs(songs)
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
