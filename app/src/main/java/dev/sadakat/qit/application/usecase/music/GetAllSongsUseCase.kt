package dev.sadakat.qit.application.usecase.music

import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all songs
 */
class GetAllSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    operator fun invoke(): Flow<List<Song>> {
        return musicRepository.getAllSongs()
    }
}
