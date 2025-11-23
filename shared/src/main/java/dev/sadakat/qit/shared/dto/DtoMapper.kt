package dev.sadakat.qit.shared.dto

import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.DownloadStatus
import dev.sadakat.qit.shared.domain.valueobject.Duration
import dev.sadakat.qit.shared.domain.valueobject.FileSize

/**
 * Mappers between Domain models and DTOs
 */

// Song mapping
fun Song.toDto(): SongDto {
    return SongDto(
        id = id.value,
        title = title,
        artist = artist,
        album = album,
        durationMs = duration.milliseconds,
        filePath = filePath,
        uri = uri,
        coverArtUri = coverArtUri,
        fileSizeBytes = fileSize.bytes,
        mimeType = mimeType,
        bitrate = bitrate,
        dateAdded = dateAdded
    )
}

fun SongDto.toDomain(): Song {
    return Song(
        id = SongId.from(id),
        title = title,
        artist = artist,
        album = album,
        duration = Duration.fromMilliseconds(durationMs),
        filePath = filePath,
        uri = uri,
        coverArtUri = coverArtUri,
        fileSize = FileSize.fromBytes(fileSizeBytes),
        mimeType = mimeType,
        bitrate = bitrate,
        dateAdded = dateAdded,
        downloadStatus = DownloadStatus.NotDownloaded
    )
}

// Playlist mapping
fun Playlist.toDto(): PlaylistDto {
    return PlaylistDto(
        id = id.value,
        name = name,
        description = description,
        songIds = getSongIds().map { it.value },
        createdAt = createdAt,
        updatedAt = updatedAt,
        coverArtUri = coverArtUri
    )
}

fun PlaylistDto.toDomain(): Playlist {
    return Playlist(
        id = PlaylistId.from(id),
        name = name,
        description = description,
        songIds = songIds.map { SongId.from(it) },
        createdAt = createdAt,
        updatedAt = updatedAt,
        coverArtUri = coverArtUri
    )
}

// Audio Quality mapping
fun AudioQuality.toDto(): String {
    return when (this) {
        AudioQuality.LOW -> "LOW"
        AudioQuality.MEDIUM -> "MEDIUM"
        AudioQuality.HIGH -> "HIGH"
        AudioQuality.ORIGINAL -> "ORIGINAL"
    }
}

fun String.toAudioQuality(): AudioQuality {
    return when (this.uppercase()) {
        "LOW" -> AudioQuality.LOW
        "MEDIUM" -> AudioQuality.MEDIUM
        "HIGH" -> AudioQuality.HIGH
        "ORIGINAL" -> AudioQuality.ORIGINAL
        else -> AudioQuality.MEDIUM
    }
}

// Collection mappers
fun List<Song>.toSongDtos(): List<SongDto> = map { it.toDto() }
fun List<SongDto>.toSongs(): List<Song> = map { it.toDomain() }

fun List<Playlist>.toPlaylistDtos(): List<PlaylistDto> = map { it.toDto() }
fun List<PlaylistDto>.toPlaylists(): List<Playlist> = map { it.toDomain() }
