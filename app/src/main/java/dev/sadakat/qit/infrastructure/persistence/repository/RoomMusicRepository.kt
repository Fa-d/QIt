package dev.sadakat.qit.infrastructure.persistence.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dev.sadakat.qit.data.local.dao.SongDao
import dev.sadakat.qit.data.local.entity.SongEntity
import dev.sadakat.qit.infrastructure.persistence.mapper.SongMapper
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.valueobject.Duration
import dev.sadakat.qit.shared.domain.valueobject.FileSize
import kotlinx.coroutines.Dispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Room-based implementation of MusicRepository
 * Handles MediaStore scanning and database operations
 */
class RoomMusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) : MusicRepository {

    override suspend fun scanMusicLibrary(): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val songs = mutableListOf<Song>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
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

                    val song = Song(
                        id = SongId.from(id.toString()),
                        title = cursor.getString(titleColumn),
                        artist = cursor.getString(artistColumn)?.takeIf { it != "<unknown>" },
                        album = cursor.getString(albumColumn)?.takeIf { it != "<unknown>" },
                        duration = Duration.fromMilliseconds(cursor.getLong(durationColumn)),
                        filePath = cursor.getString(dataColumn),
                        uri = contentUri.toString(),
                        coverArtUri = albumArtUri.toString(),
                        fileSize = FileSize.fromBytes(cursor.getLong(sizeColumn)),
                        mimeType = cursor.getString(mimeTypeColumn),
                        bitrate = 0, // Not available from MediaStore
                        dateAdded = cursor.getLong(dateAddedColumn) * 1000
                    )
                    songs.add(song)
                }
            }

            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSongById(id: SongId): Result<Song?> {
        return try {
            val entity = songDao.getSongById(id.value)
            val song = entity?.let { SongMapper.toDomain(it) }
            Result.success(song)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSongsByIds(ids: List<SongId>): Result<List<Song>> {
        return try {
            val idStrings = ids.map { it.value }
            val entities = songDao.getSongsByIds(idStrings)
            val songs = SongMapper.toDomainList(entities)
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllSongs(): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            SongMapper.toDomainList(entities)
        }
    }

    override fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs("%$query%").map { entities ->
            SongMapper.toDomainList(entities)
        }
    }

    override fun getSongsByArtist(artist: String): Flow<List<Song>> {
        return songDao.getSongsByArtist(artist).map { entities ->
            SongMapper.toDomainList(entities)
        }
    }

    override fun getSongsByAlbum(album: String): Flow<List<Song>> {
        return songDao.getSongsByAlbum(album).map { entities ->
            SongMapper.toDomainList(entities)
        }
    }

    override suspend fun saveSong(song: Song): Result<Unit> {
        return try {
            val entity = SongMapper.toEntity(song)
            songDao.insertSong(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveSongs(songs: List<Song>): Result<Unit> {
        return try {
            val entities = SongMapper.toEntityList(songs)
            songDao.insertSongs(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSong(id: SongId): Result<Unit> {
        return try {
            songDao.deleteSong(id.value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllArtists(): Flow<List<String>> {
        return songDao.getAllArtists()
    }

    override fun getAllAlbums(): Flow<List<String>> {
        return songDao.getAllAlbums()
    }

    override fun observeSong(id: SongId): Flow<Song?> {
        return songDao.observeSong(id.value).map { entity ->
            entity?.let { SongMapper.toDomain(it) }
        }
    }

    override suspend fun updateDownloadStatus(id: SongId, downloadPath: String?): Result<Unit> {
        return try {
            val isDownloaded = downloadPath != null
            songDao.updateDownloadStatus(id.value, isDownloaded)
            // TODO: Update watchFilePath as well
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDownloadedSongs(): Flow<List<Song>> {
        return songDao.getDownloadedSongs().map { entities ->
            SongMapper.toDomainList(entities)
        }
    }
}
