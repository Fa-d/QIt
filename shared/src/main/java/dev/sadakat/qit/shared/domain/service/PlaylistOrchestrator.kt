package dev.sadakat.qit.shared.domain.service

import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.valueobject.Duration
import dev.sadakat.qit.shared.domain.valueobject.FileSize

/**
 * Domain Service for complex playlist operations
 * Coordinates between Playlist entity and repositories
 */
class PlaylistOrchestrator(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository
) {

    /**
     * Creates a new playlist and saves it
     */
    suspend fun createPlaylist(
        name: String,
        description: String? = null,
        coverArtUri: String? = null
    ): Result<Playlist> {
        return try {
            val playlist = Playlist.create(
                name = name,
                description = description,
                coverArtUri = coverArtUri
            )
            playlistRepository.savePlaylist(playlist).map { playlist }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds a song to a playlist with validation
     */
    suspend fun addSongToPlaylist(
        playlistId: PlaylistId,
        songId: SongId
    ): Result<Playlist> {
        return try {
            // Get playlist
            val playlistResult = playlistRepository.getPlaylistById(playlistId)
            val playlist = playlistResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Playlist not found"))

            // Verify song exists
            val songResult = musicRepository.getSongById(songId)
            if (songResult.getOrNull() == null) {
                return Result.failure(IllegalStateException("Song not found"))
            }

            // Add song to playlist
            val updatedPlaylist = playlist.addSong(songId)
            playlistRepository.savePlaylist(updatedPlaylist).map { updatedPlaylist }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes a song from a playlist
     */
    suspend fun removeSongFromPlaylist(
        playlistId: PlaylistId,
        songId: SongId
    ): Result<Playlist> {
        return try {
            val playlistResult = playlistRepository.getPlaylistById(playlistId)
            val playlist = playlistResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Playlist not found"))

            val updatedPlaylist = playlist.removeSong(songId)
            playlistRepository.savePlaylist(updatedPlaylist).map { updatedPlaylist }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculates total duration of all songs in a playlist
     */
    suspend fun calculatePlaylistDuration(playlistId: PlaylistId): Result<Duration> {
        return try {
            val songs = playlistRepository.getSongsForPlaylist(playlistId).getOrNull()
                ?: return Result.failure(IllegalStateException("Failed to get songs"))

            val totalDuration = songs.fold(Duration.ZERO) { acc, song ->
                acc + song.duration
            }
            Result.success(totalDuration)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculates total size of all songs in a playlist
     */
    suspend fun calculatePlaylistSize(playlistId: PlaylistId): Result<FileSize> {
        return try {
            val songs = playlistRepository.getSongsForPlaylist(playlistId).getOrNull()
                ?: return Result.failure(IllegalStateException("Failed to get songs"))

            val totalSize = songs.fold(FileSize.ZERO) { acc, song ->
                acc + song.fileSize
            }
            Result.success(totalSize)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reorders a song within a playlist
     */
    suspend fun reorderSong(
        playlistId: PlaylistId,
        songId: SongId,
        toIndex: Int
    ): Result<Playlist> {
        return try {
            val playlistResult = playlistRepository.getPlaylistById(playlistId)
            val playlist = playlistResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Playlist not found"))

            val updatedPlaylist = playlist.moveSong(songId, toIndex)
            playlistRepository.savePlaylist(updatedPlaylist).map { updatedPlaylist }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds multiple songs to a playlist in batch
     */
    suspend fun addMultipleSongs(
        playlistId: PlaylistId,
        songIds: List<SongId>
    ): Result<Playlist> {
        return try {
            val playlistResult = playlistRepository.getPlaylistById(playlistId)
            val playlist = playlistResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Playlist not found"))

            // Verify all songs exist
            val songsResult = musicRepository.getSongsByIds(songIds)
            val songs = songsResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Failed to verify songs"))

            if (songs.size != songIds.size) {
                return Result.failure(IllegalStateException("Some songs not found"))
            }

            val updatedPlaylist = playlist.addSongs(songIds)
            playlistRepository.savePlaylist(updatedPlaylist).map { updatedPlaylist }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates playlist metadata
     */
    suspend fun updatePlaylistMetadata(
        playlistId: PlaylistId,
        name: String? = null,
        description: String? = null,
        coverArtUri: String? = null
    ): Result<Playlist> {
        return try {
            val playlistResult = playlistRepository.getPlaylistById(playlistId)
            val playlist = playlistResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Playlist not found"))

            val updatedPlaylist = playlist.updateMetadata(
                newName = name,
                newDescription = description,
                newCoverArtUri = coverArtUri
            )
            playlistRepository.savePlaylist(updatedPlaylist).map { updatedPlaylist }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a playlist
     */
    suspend fun deletePlaylist(playlistId: PlaylistId): Result<Unit> {
        return playlistRepository.deletePlaylist(playlistId)
    }

    /**
     * Gets playlist with its songs
     */
    suspend fun getPlaylistWithSongs(playlistId: PlaylistId): Result<Pair<Playlist, List<Song>>> {
        return try {
            val playlistResult = playlistRepository.getPlaylistById(playlistId)
            val playlist = playlistResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Playlist not found"))

            val songsResult = playlistRepository.getSongsForPlaylist(playlistId)
            val songs = songsResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Failed to get songs"))

            Result.success(Pair(playlist, songs))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
