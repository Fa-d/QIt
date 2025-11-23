package dev.sadakat.qit.application.usecase.playlist

import dev.sadakat.qit.application.usecase.UseCaseWithParams
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.event.SongAddedToPlaylist
import dev.sadakat.qit.shared.domain.service.PlaylistOrchestrator
import javax.inject.Inject

/**
 * Use case for adding a song to a playlist
 */
class AddSongToPlaylistUseCase @Inject constructor(
    private val playlistOrchestrator: PlaylistOrchestrator,
    private val eventPublisher: DomainEventPublisher
) : UseCaseWithParams<AddSongToPlaylistUseCase.Params, Playlist>() {

    override suspend fun invoke(params: Params): Result<Playlist> {
        return try {
            val result = playlistOrchestrator.addSongToPlaylist(
                playlistId = params.playlistId,
                songId = params.songId
            )

            result.onSuccess {
                eventPublisher.publish(
                    SongAddedToPlaylist(
                        playlistId = params.playlistId,
                        songId = params.songId
                    )
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class Params(
        val playlistId: PlaylistId,
        val songId: SongId
    )
}
