package dev.sadakat.qit.data.repository

import dev.sadakat.qit.data.local.dao.PlaylistDao
import dev.sadakat.qit.data.local.dao.SongDao
import dev.sadakat.qit.data.local.entity.PlaylistEntity
import dev.sadakat.qit.shared.model.Playlist
import dev.sadakat.qit.shared.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for managing playlists
 */
class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao
) {

    /**
     * Get all playlists
     */
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { it.toPlaylist() }
        }
    }

    /**
     * Get a specific playlist by ID
     */
    suspend fun getPlaylistById(playlistId: String): Playlist? {
        return playlistDao.getPlaylistById(playlistId)?.toPlaylist()
    }

    /**
     * Observe a specific playlist by ID
     */
    fun observePlaylistById(playlistId: String): Flow<Playlist?> {
        return playlistDao.observePlaylistById(playlistId).map { it?.toPlaylist() }
    }

    /**
     * Get songs for a playlist
     */
    suspend fun getSongsForPlaylist(playlistId: String): List<Song> {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return emptyList()
        val songIds = if (playlist.songIds.isEmpty()) {
            emptyList()
        } else {
            playlist.songIds.split(",")
        }
        return songDao.getSongsByIds(songIds).map { it.toSong() }
    }

    /**
     * Create a new playlist
     */
    suspend fun createPlaylist(name: String, description: String? = null): Playlist {
        val playlist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            songIds = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(PlaylistEntity.fromPlaylist(playlist))
        return playlist
    }

    /**
     * Update playlist
     */
    suspend fun updatePlaylist(playlist: Playlist) {
        val entity = PlaylistEntity.fromPlaylist(
            playlist.copy(updatedAt = System.currentTimeMillis())
        )
        playlistDao.updatePlaylist(entity)
    }

    /**
     * Add song to playlist
     */
    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        val currentSongIds = if (playlist.songIds.isEmpty()) {
            emptyList()
        } else {
            playlist.songIds.split(",")
        }

        if (!currentSongIds.contains(songId)) {
            val updatedSongIds = (currentSongIds + songId).joinToString(",")
            playlistDao.updatePlaylist(
                playlist.copy(
                    songIds = updatedSongIds,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Remove song from playlist
     */
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        val currentSongIds = if (playlist.songIds.isEmpty()) {
            emptyList()
        } else {
            playlist.songIds.split(",")
        }

        val updatedSongIds = currentSongIds.filter { it != songId }.joinToString(",")
        playlistDao.updatePlaylist(
            playlist.copy(
                songIds = updatedSongIds,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Add multiple songs to playlist
     */
    suspend fun addSongsToPlaylist(playlistId: String, songIds: List<String>) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        val currentSongIds = if (playlist.songIds.isEmpty()) {
            emptyList()
        } else {
            playlist.songIds.split(",")
        }

        val uniqueNewSongs = songIds.filter { it !in currentSongIds }
        if (uniqueNewSongs.isNotEmpty()) {
            val updatedSongIds = (currentSongIds + uniqueNewSongs).joinToString(",")
            playlistDao.updatePlaylist(
                playlist.copy(
                    songIds = updatedSongIds,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Delete a playlist
     */
    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylistById(playlistId)
    }

    /**
     * Reorder songs in a playlist
     */
    suspend fun reorderSongs(playlistId: String, newOrder: List<String>) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        playlistDao.updatePlaylist(
            playlist.copy(
                songIds = newOrder.joinToString(","),
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
