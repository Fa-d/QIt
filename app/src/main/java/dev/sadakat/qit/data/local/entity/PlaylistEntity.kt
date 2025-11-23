package dev.sadakat.qit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.sadakat.qit.shared.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val songIds: String = "", // Comma-separated list of song IDs
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val coverArtUri: String? = null
) {
    fun toPlaylist(): Playlist {
        return Playlist(
            id = id,
            name = name,
            description = description,
            songIds = if (songIds.isEmpty()) emptyList() else songIds.split(","),
            createdAt = createdAt,
            updatedAt = updatedAt,
            coverArtUri = coverArtUri
        )
    }

    companion object {
        fun fromPlaylist(playlist: Playlist): PlaylistEntity {
            return PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                description = playlist.description,
                songIds = playlist.songIds.joinToString(","),
                createdAt = playlist.createdAt,
                updatedAt = playlist.updatedAt,
                coverArtUri = playlist.coverArtUri
            )
        }
    }
}
