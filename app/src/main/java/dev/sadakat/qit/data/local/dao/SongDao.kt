package dev.sadakat.qit.data.local.dao

import androidx.room.*
import dev.sadakat.qit.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id = :songId")
    fun observeSongById(songId: String): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE id = :songId")
    fun observeSong(songId: String): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE id IN (:songIds)")
    suspend fun getSongsByIds(songIds: List<String>): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id IN (:songIds)")
    fun observeSongsByIds(songIds: List<String>): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY title ASC")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY title ASC")
    fun getSongsByAlbum(album: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE title LIKE :query OR artist LIKE :query OR album LIKE :query ORDER BY title ASC")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isDownloadedOnWatch = 1")
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET isDownloadedOnWatch = :isDownloaded WHERE id = :songId")
    suspend fun updateDownloadStatus(songId: String, isDownloaded: Boolean)

    @Query("UPDATE songs SET watchFilePath = :watchFilePath WHERE id = :songId")
    suspend fun updateWatchFilePath(songId: String, watchFilePath: String?)

    @Delete
    suspend fun deleteSongEntity(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSong(songId: String)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query("SELECT DISTINCT artist FROM songs WHERE artist IS NOT NULL ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    @Query("SELECT DISTINCT album FROM songs WHERE album IS NOT NULL ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<String>>
}
