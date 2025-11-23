package dev.sadakat.qit.wear.application.usecase.playlist

import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all playlists on watch
 */
class GetAllPlaylistsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(): Flow<List<Playlist>> {
        return playlistRepository.getAllPlaylists()
    }
}
