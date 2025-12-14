package dev.sadakat.qit.wear.application.usecase.download

import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to clear all downloaded songs from the watch
 */
@Singleton
class ClearAllDownloadsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(): Result<Int> {
        return try {
            // Get all songs to find which ones are downloaded
            val allSongs = musicRepository.getAllSongs().first()
            var clearedCount = 0

            allSongs.forEach { song ->
                // Check if song is downloaded on watch
                if (song.isAvailableOnWatch()) {
                    val result = downloadRepository.deleteDownload(song.id)
                    if (result.isSuccess) {
                        clearedCount++
                    }
                }
            }

            Result.success(clearedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}