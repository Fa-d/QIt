package dev.sadakat.qit.infrastructure.persistence.repository

import dev.sadakat.qit.data.local.dao.PlaylistDao
import dev.sadakat.qit.data.local.dao.SongDao
import dev.sadakat.qit.infrastructure.persistence.mapper.PlaylistMapper
import dev.sadakat.qit.infrastructure.persistence.mapper.SongMapper
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-based implementation of PlaylistRepository
 */
class RoomPlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao
) : PlaylistRepository {

    override suspend fun getPlaylistById(id: PlaylistId): Result<Playlist?> {
        return try {
            val entity = playlistDao.getPlaylistById(id.value)
            val playlist = entity?.let { PlaylistMapper.toDomain(it) }
            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            PlaylistMapper.toDomainList(entities)
        }
    }

    override suspend fun savePlaylist(playlist: Playlist): Result<Unit> {
        return try {
            val entity = PlaylistMapper.toEntity(playlist)
            playlistDao.insertPlaylist(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePlaylist(id: PlaylistId): Result<Unit> {
        return try {
            playlistDao.deletePlaylist(id.value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSongsForPlaylist(playlistId: PlaylistId): Result<List<Song>> {
        return try {
            val playlist = playlistDao.getPlaylistById(playlistId.value)
                ?: return Result.success(emptyList())

            val songIds = if (playlist.songIds.isBlank()) {
                emptyList()
            } else {
                playlist.songIds.split(",")
            }

            if (songIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val entities = songDao.getSongsByIds(songIds)
            val songs = SongMapper.toDomainList(entities)
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observePlaylist(id: PlaylistId): Flow<Playlist?> {
        return playlistDao.observePlaylistById(id.value).map { entity ->
            entity?.let { PlaylistMapper.toDomain(it) }
        }
    }

    override fun observeSongsForPlaylist(playlistId: PlaylistId): Flow<List<Song>> {
        return playlistDao.observePlaylistById(playlistId.value).map { playlist ->
            if (playlist == null || playlist.songIds.isBlank()) {
                emptyList()
            } else {
                val songIds = playlist.songIds.split(",")
                // This is a simplification - in production, use a proper join query
                val entities = songDao.getSongsByIds(songIds)
                SongMapper.toDomainList(entities)
            }
        }
    }

    override fun searchPlaylists(query: String): Flow<List<Playlist>> {
        return playlistDao.searchPlaylists("%$query%").map { entities ->
            PlaylistMapper.toDomainList(entities)
        }
    }

    override suspend fun getPlaylistsContainingSong(songId: SongId): Result<List<Playlist>> {
        return try {
            val allPlaylists = mutableListOf<Playlist>()
            playlistDao.getAllPlaylists().collect { entities ->
                allPlaylists.clear()
                allPlaylists.addAll(PlaylistMapper.toDomainList(entities))
            }

            val filtered = allPlaylists.filter { playlist ->
                playlist.containsSong(songId)
            }

            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
