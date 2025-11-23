package dev.sadakat.qit.application.usecase.music

import dev.sadakat.qit.application.usecase.UseCaseWithParams
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for searching songs
 */
class SearchSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    operator fun invoke(query: String): Flow<List<Song>> {
        return if (query.isBlank()) {
            musicRepository.getAllSongs()
        } else {
            musicRepository.searchSongs(query)
        }
    }
}
