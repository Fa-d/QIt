package dev.sadakat.qit.wear.infrastructure.repository

import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.wear.data.local.dao.SongDao
import dev.sadakat.qit.wear.infrastructure.mapper.SongMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Wear implementation of MusicRepository
 * Adapts SongDao to domain MusicRepository interface
 */
class WearMusicRepository @Inject constructor(
    private val songDao: SongDao
) : MusicRepository {

    /**
     * Not supported on wear - music library scanning happens on phone
     */
    override suspend fun scanMusicLibrary(): Result<List<Song>> {
        return Result.failure(
            UnsupportedOperationException("Music library scanning is only available on phone")
        )
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
            val entities = songDao.getSongsByIds(ids.map { it.value })
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
        // Wear SongDao doesn't have a search method, so we filter in memory
        return songDao.getAllSongs().map { entities ->
            val songs = SongMapper.toDomainList(entities)
            songs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                song.artist?.contains(query, ignoreCase = true) == true ||
                song.album?.contains(query, ignoreCase = true) == true
            }
        }
    }

    override fun getSongsByArtist(artist: String): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            val songs = SongMapper.toDomainList(entities)
            songs.filter { it.artist == artist }
        }
    }

    override fun getSongsByAlbum(album: String): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            val songs = SongMapper.toDomainList(entities)
            songs.filter { it.album == album }
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
            songDao.deleteSongById(id.value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllArtists(): Flow<List<String>> {
        return songDao.getAllSongs().map { entities ->
            entities.mapNotNull { it.artist }.distinct().sorted()
        }
    }

    override fun getAllAlbums(): Flow<List<String>> {
        return songDao.getAllSongs().map { entities ->
            entities.mapNotNull { it.album }.distinct().sorted()
        }
    }

    override fun observeSong(id: SongId): Flow<Song?> {
        return songDao.observeSongById(id.value).map { entity ->
            entity?.let { SongMapper.toDomain(it) }
        }
    }

    override suspend fun updateDownloadStatus(id: SongId, downloadPath: String?): Result<Unit> {
        return try {
            songDao.updateDownloadStatus(
                songId = id.value,
                isDownloaded = downloadPath != null,
                filePath = downloadPath
            )
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
