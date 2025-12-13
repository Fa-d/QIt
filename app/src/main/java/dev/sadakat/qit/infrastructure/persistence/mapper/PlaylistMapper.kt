package dev.sadakat.qit.infrastructure.persistence.mapper

import dev.sadakat.qit.data.local.entity.PlaylistEntity
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.SongId

/**
 * Mapper between Room PlaylistEntity and Domain Playlist
 */
object PlaylistMapper {

    fun toDomain(entity: PlaylistEntity): Playlist {
        val songIds = if (entity.songIds.isBlank()) {
            emptyList()
        } else {
            entity.songIds.split(",").map { SongId.from(it) }
        }

        return Playlist(
            id = PlaylistId.from(entity.id),
            name = entity.name,
            description = entity.description,
            songIds = songIds,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            coverArtUri = entity.coverArtUri
        )
    }

    fun toEntity(playlist: Playlist): PlaylistEntity {
        val songIdsStr = playlist.getSongIds().joinToString(",") { it.value }

        return PlaylistEntity(
            id = playlist.id.value,
            name = playlist.name,
            description = playlist.description,
            songIds = songIdsStr,
            createdAt = playlist.createdAt,
            updatedAt = playlist.updatedAt,
            coverArtUri = playlist.coverArtUri
        )
    }

    fun toDomainList(entities: List<PlaylistEntity>): List<Playlist> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(playlists: List<Playlist>): List<PlaylistEntity> {
        return playlists.map { toEntity(it) }
    }
}
