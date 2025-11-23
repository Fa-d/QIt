package dev.sadakat.qit.wear.application.usecase.download

import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all downloaded songs
 */
class GetDownloadedSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    operator fun invoke(): Flow<List<Song>> {
        return musicRepository.getDownloadedSongs()
    }
}
