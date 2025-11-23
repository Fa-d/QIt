package dev.sadakat.qit.data.local.entity

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
    val filePath: String? = null,
    val uri: String? = null,
    val coverArtUri: String? = null,
    val isDownloadedOnWatch: Boolean = false,
    val watchFilePath: String? = null,
    val fileSize: Long = 0L,
    val mimeType: String? = null,
    val bitrate: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
) {
    fun toSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            filePath = filePath,
            uri = uri,
            coverArtUri = coverArtUri,
            isDownloadedOnWatch = isDownloadedOnWatch,
            watchFilePath = watchFilePath,
            fileSize = fileSize,
            mimeType = mimeType,
            bitrate = bitrate,
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
                filePath = song.filePath,
                uri = song.uri,
                coverArtUri = song.coverArtUri,
                isDownloadedOnWatch = song.isDownloadedOnWatch,
                watchFilePath = song.watchFilePath,
                fileSize = song.fileSize,
                mimeType = song.mimeType,
                bitrate = song.bitrate,
                dateAdded = song.dateAdded
            )
        }
    }
}
