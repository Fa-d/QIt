package dev.sadakat.qit.shared.domain.repository

import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for Playlist operations
 */
interface PlaylistRepository {

    /**
     * Gets a playlist by ID
     */
    suspend fun getPlaylistById(id: PlaylistId): Result<Playlist?>

    /**
     * Gets all playlists
     */
    fun getAllPlaylists(): Flow<List<Playlist>>

    /**
     * Saves a playlist
     */
    suspend fun savePlaylist(playlist: Playlist): Result<Unit>

    /**
     * Deletes a playlist
     */
    suspend fun deletePlaylist(id: PlaylistId): Result<Unit>

    /**
     * Gets songs for a specific playlist
     */
    suspend fun getSongsForPlaylist(playlistId: PlaylistId): Result<List<Song>>

    /**
     * Observes changes to a specific playlist
     */
    fun observePlaylist(id: PlaylistId): Flow<Playlist?>

    /**
     * Observes songs for a specific playlist
     */
    fun observeSongsForPlaylist(playlistId: PlaylistId): Flow<List<Song>>

    /**
     * Searches playlists by name
     */
    fun searchPlaylists(query: String): Flow<List<Playlist>>

    /**
     * Gets playlists that contain a specific song
     */
    suspend fun getPlaylistsContainingSong(songId: SongId): Result<List<Playlist>>
}
