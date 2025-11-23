package dev.sadakat.qit.wear.data.repository

import dev.sadakat.qit.shared.model.Playlist
import dev.sadakat.qit.shared.model.Song
import dev.sadakat.qit.wear.data.local.dao.PlaylistDao
import dev.sadakat.qit.wear.data.local.dao.SongDao
import dev.sadakat.qit.wear.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing playlists on the watch
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
     * Get downloaded songs for a playlist
     */
    suspend fun getDownloadedSongsForPlaylist(playlistId: String): List<Song> {
        val allSongs = getSongsForPlaylist(playlistId)
        return allSongs.filter { it.isDownloadedOnWatch }
    }

    /**
     * Save playlists (typically synced from phone)
     */
    suspend fun savePlaylists(playlists: List<Playlist>) {
        playlistDao.insertPlaylists(playlists.map { PlaylistEntity.fromPlaylist(it) })
    }

    /**
     * Update playlist
     */
    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(PlaylistEntity.fromPlaylist(playlist))
    }

    /**
     * Delete a playlist
     */
    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.getPlaylistById(playlistId)?.let {
            playlistDao.deletePlaylist(it)
        }
    }
}
