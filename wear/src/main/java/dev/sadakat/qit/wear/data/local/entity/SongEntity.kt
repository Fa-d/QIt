package dev.sadakat.qit.wear.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.DownloadStatus
import dev.sadakat.qit.shared.domain.valueobject.Duration
import dev.sadakat.qit.shared.domain.valueobject.FileSize
import dev.sadakat.qit.shared.model.Song
import dev.sadakat.qit.shared.domain.entity.Song as DomainSong

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val duration: Long = 0L,
    val localFilePath: String? = null, // Path on watch if downloaded
    val isDownloaded: Boolean = false,
    val fileSize: Long = 0L,
    val downloadProgress: Float = 0f,
    val dateAdded: Long = System.currentTimeMillis()
) {
    fun toSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            filePath = null,
            uri = null,
            coverArtUri = null,
            isDownloadedOnWatch = isDownloaded,
            watchFilePath = localFilePath,
            fileSize = fileSize,
            mimeType = null,
            bitrate = 0,
            dateAdded = dateAdded
        )
    }

    fun toDomainSong(): DomainSong {
        val downloadStatus = if (isDownloaded && localFilePath != null) {
            DownloadStatus.Downloaded(localFilePath)
        } else if (downloadProgress > 0f && downloadProgress < 1f) {
            DownloadStatus.Downloading(downloadProgress)
        } else {
            DownloadStatus.NotDownloaded
        }

        return DomainSong(
            id = SongId.from(id),
            title = title,
            artist = artist,
            album = album,
            duration = Duration.fromMilliseconds(duration),
            filePath = null,
            uri = null,
            coverArtUri = null,
            fileSize = FileSize.fromBytes(fileSize),
            mimeType = null,
            bitrate = 0,
            dateAdded = dateAdded,
            downloadStatus = downloadStatus
        )
    }

    companion object {
        fun fromSong(song: Song): SongEntity {
            return SongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                localFilePath = song.watchFilePath,
                isDownloaded = song.isDownloadedOnWatch,
                fileSize = song.fileSize,
                dateAdded = song.dateAdded
            )
        }
    }
}
