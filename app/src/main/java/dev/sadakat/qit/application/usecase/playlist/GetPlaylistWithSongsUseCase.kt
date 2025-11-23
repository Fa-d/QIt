package dev.sadakat.qit.application.usecase.playlist

import dev.sadakat.qit.application.usecase.UseCaseWithParams
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.service.PlaylistOrchestrator
import javax.inject.Inject

/**
 * Use case for getting a playlist with its songs
 */
class GetPlaylistWithSongsUseCase @Inject constructor(
    private val playlistOrchestrator: PlaylistOrchestrator
) : UseCaseWithParams<PlaylistId, Pair<Playlist, List<Song>>>() {

    override suspend fun invoke(params: PlaylistId): Result<Pair<Playlist, List<Song>>> {
        return playlistOrchestrator.getPlaylistWithSongs(params)
    }
}
