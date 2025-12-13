package dev.sadakat.qit.wear.infrastructure.mapper

import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.DownloadStatus
import dev.sadakat.qit.shared.domain.valueobject.Duration
import dev.sadakat.qit.shared.domain.valueobject.FileSize
import dev.sadakat.qit.wear.data.local.entity.SongEntity

/**
 * Mapper between Wear Room SongEntity and Domain Song
 */
object SongMapper {

    fun toDomain(entity: SongEntity): Song {
        val downloadStatus = when {
            entity.isDownloaded && entity.localFilePath != null ->
                DownloadStatus.Downloaded(entity.localFilePath)
            entity.downloadProgress > 0f && entity.downloadProgress < 1f ->
                DownloadStatus.Downloading(entity.downloadProgress)
            else -> DownloadStatus.NotDownloaded
        }

        return Song(
            id = SongId.from(entity.id),
            title = entity.title,
            artist = entity.artist,
            album = entity.album,
            duration = Duration.fromMilliseconds(entity.duration),
            filePath = null, // Wear doesn't have phone file path
            uri = null, // Wear doesn't have phone URI
            coverArtUri = null, // TODO: Add cover art support later
            fileSize = FileSize.fromBytes(entity.fileSize),
            mimeType = null, // TODO: Add mime type if needed
            bitrate = 0, // Wear doesn't track bitrate
            dateAdded = entity.dateAdded,
            downloadStatus = downloadStatus
        )
    }

    fun toEntity(song: Song): SongEntity {
        val (isDownloaded, localPath, progress) = when (val status = song.downloadStatus) {
            is DownloadStatus.Downloaded -> Triple(true, status.localPath, 1f)
            is DownloadStatus.Downloading -> Triple(false, null, status.progress)
            else -> Triple(false, null, 0f)
        }

        return SongEntity(
            id = song.id.value,
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration.milliseconds,
            localFilePath = localPath,
            isDownloaded = isDownloaded,
            fileSize = song.fileSize.bytes,
            downloadProgress = progress,
            dateAdded = song.dateAdded
        )
    }

    fun toDomainList(entities: List<SongEntity>): List<Song> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(songs: List<Song>): List<SongEntity> {
        return songs.map { toEntity(it) }
    }
}
