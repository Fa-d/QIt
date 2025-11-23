package dev.sadakat.qit.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dev.sadakat.qit.data.local.dao.SongDao
import dev.sadakat.qit.data.local.entity.SongEntity
import dev.sadakat.qit.shared.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository for accessing music files from the device's media store
 * and managing song metadata in the local database
 */
class MusicRepository(
    private val context: Context,
    private val songDao: SongDao
) {

    /**
     * Scan device for music files and update the database
     */
    suspend fun scanMusicLibrary(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val songs = mutableListOf<SongEntity>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA, // File path
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ALBUM_ID
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumId = cursor.getLong(albumIdColumn)
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    val song = SongEntity(
                        id = id.toString(),
                        title = cursor.getString(titleColumn),
                        artist = cursor.getString(artistColumn)?.takeIf { it != "<unknown>" },
                        album = cursor.getString(albumColumn)?.takeIf { it != "<unknown>" },
                        duration = cursor.getLong(durationColumn),
                        filePath = cursor.getString(dataColumn),
                        uri = contentUri.toString(),
                        coverArtUri = albumArtUri.toString(),
                        fileSize = cursor.getLong(sizeColumn),
                        mimeType = cursor.getString(mimeTypeColumn),
                        dateAdded = cursor.getLong(dateAddedColumn) * 1000 // Convert to milliseconds
                    )
                    songs.add(song)
                }
            }

            // Insert all songs into database
            songDao.insertSongs(songs)
            Result.success(songs.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all songs from the database
     */
    fun getAllSongs(): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }

    /**
     * Get a specific song by ID
     */
    suspend fun getSongById(songId: String): Song? {
        return songDao.getSongById(songId)?.toSong()
    }

    /**
     * Get songs by IDs
     */
    suspend fun getSongsByIds(songIds: List<String>): List<Song> {
        return songDao.getSongsByIds(songIds).map { it.toSong() }
    }

    /**
     * Get all artists
     */
    fun getAllArtists(): Flow<List<String>> {
        return songDao.getAllArtists()
    }

    /**
     * Get all albums
     */
    fun getAllAlbums(): Flow<List<String>> {
        return songDao.getAllAlbums()
    }

    /**
     * Get songs by artist
     */
    fun getSongsByArtist(artist: String): Flow<List<Song>> {
        return songDao.getSongsByArtist(artist).map { entities ->
            entities.map { it.toSong() }
        }
    }

    /**
     * Get songs by album
     */
    fun getSongsByAlbum(album: String): Flow<List<Song>> {
        return songDao.getSongsByAlbum(album).map { entities ->
            entities.map { it.toSong() }
        }
    }

    /**
     * Update song download status for watch
     */
    suspend fun updateDownloadStatus(songId: String, isDownloaded: Boolean) {
        songDao.updateDownloadStatus(songId, isDownloaded)
    }
}
