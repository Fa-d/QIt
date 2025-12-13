package dev.sadakat.qit.infrastructure.persistence.mapper

import dev.sadakat.qit.data.local.entity.SongEntity
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.DownloadStatus
import dev.sadakat.qit.shared.domain.valueobject.Duration
import dev.sadakat.qit.shared.domain.valueobject.FileSize

/**
 * Mapper between Room SongEntity and Domain Song
 */
object SongMapper {

    fun toDomain(entity: SongEntity): Song {
        val downloadStatus = when {
            entity.isDownloadedOnWatch && entity.watchFilePath != null ->
                DownloadStatus.Downloaded(entity.watchFilePath)
            else -> DownloadStatus.NotDownloaded
        }

        return Song(
            id = SongId.from(entity.id),
            title = entity.title,
            artist = entity.artist,
            album = entity.album,
            duration = Duration.fromMilliseconds(entity.duration),
            filePath = entity.filePath,
            uri = entity.uri,
            coverArtUri = entity.coverArtUri,
            fileSize = FileSize.fromBytes(entity.fileSize),
            mimeType = entity.mimeType,
            bitrate = entity.bitrate,
            dateAdded = entity.dateAdded,
            downloadStatus = downloadStatus
        )
    }

    fun toEntity(song: Song): SongEntity {
        val (isDownloaded, watchPath) = when (val status = song.downloadStatus) {
            is DownloadStatus.Downloaded -> Pair(true, status.localPath)
            else -> Pair(false, null)
        }

        return SongEntity(
            id = song.id.value,
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration.milliseconds,
            filePath = song.filePath,
            uri = song.uri,
            coverArtUri = song.coverArtUri,
            isDownloadedOnWatch = isDownloaded,
            watchFilePath = watchPath,
            fileSize = song.fileSize.bytes,
            mimeType = song.mimeType,
            bitrate = song.bitrate,
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
