package dev.sadakat.qit.shared.domain.repository

import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for Music operations
 * Infrastructure layer will provide the implementation
 */
interface MusicRepository {

    /**
     * Scans the device's music library and returns discovered songs
     */
    suspend fun scanMusicLibrary(): Result<List<Song>>

    /**
     * Gets a song by its ID
     */
    suspend fun getSongById(id: SongId): Result<Song?>

    /**
     * Gets songs by their IDs
     */
    suspend fun getSongsByIds(ids: List<SongId>): Result<List<Song>>

    /**
     * Gets all songs
     */
    fun getAllSongs(): Flow<List<Song>>

    /**
     * Searches songs by query (title, artist, album)
     */
    fun searchSongs(query: String): Flow<List<Song>>

    /**
     * Gets songs by artist
     */
    fun getSongsByArtist(artist: String): Flow<List<Song>>

    /**
     * Gets songs by album
     */
    fun getSongsByAlbum(album: String): Flow<List<Song>>

    /**
     * Saves a song
     */
    suspend fun saveSong(song: Song): Result<Unit>

    /**
     * Saves multiple songs
     */
    suspend fun saveSongs(songs: List<Song>): Result<Unit>

    /**
     * Deletes a song
     */
    suspend fun deleteSong(id: SongId): Result<Unit>

    /**
     * Gets all artists
     */
    fun getAllArtists(): Flow<List<String>>

    /**
     * Gets all albums
     */
    fun getAllAlbums(): Flow<List<String>>

    /**
     * Observes changes to a specific song
     */
    fun observeSong(id: SongId): Flow<Song?>

    /**
     * Updates a song's download status
     */
    suspend fun updateDownloadStatus(id: SongId, downloadPath: String?): Result<Unit>

    /**
     * Gets downloaded songs only (for watch)
     */
    fun getDownloadedSongs(): Flow<List<Song>>
}
