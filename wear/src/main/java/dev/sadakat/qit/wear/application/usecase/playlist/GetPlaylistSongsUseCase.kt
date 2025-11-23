package dev.sadakat.qit.wear.application.usecase.playlist

import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing songs in a playlist
 */
class GetPlaylistSongsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(playlistId: PlaylistId): Flow<List<Song>> {
        return playlistRepository.observeSongsForPlaylist(playlistId)
    }
}
