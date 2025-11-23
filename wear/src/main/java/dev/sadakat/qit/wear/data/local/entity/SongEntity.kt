package dev.sadakat.qit.wear.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.sadakat.qit.shared.model.Song

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
