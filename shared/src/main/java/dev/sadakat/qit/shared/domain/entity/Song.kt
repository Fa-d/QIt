package dev.sadakat.qit.shared.domain.entity

import dev.sadakat.qit.shared.domain.valueobject.*

/**
 * Domain Entity representing a Song
 * Rich domain model with behavior and business rules
 */
data class Song(
    val id: SongId,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Duration,
    val filePath: String?, // Path on phone
    val uri: String?, // Content URI on phone
    val coverArtUri: String?,
    val fileSize: FileSize,
    val mimeType: String?,
    val bitrate: Int, // Original bitrate in kbps
    val dateAdded: Long,
    val downloadStatus: DownloadStatus = DownloadStatus.NotDownloaded
) {

    init {
        require(title.isNotBlank()) { "Song title cannot be blank" }
    }

    /**
     * Returns the display name for the artist
     */
    fun artistName(): String = artist ?: "Unknown Artist"

    /**
     * Returns the display name for the album
     */
    fun albumName(): String = album ?: "Unknown Album"

    /**
     * Checks if the song is available for playback on the watch
     */
    fun isAvailableOnWatch(): Boolean = downloadStatus.isDownloaded()

    /**
     * Checks if the song requires phone connection for streaming
     */
    fun requiresPhoneForStreaming(): Boolean = !isAvailableOnWatch()

    /**
     * Returns the estimated file size for a given audio quality
     */
    fun estimatedSizeForQuality(quality: AudioQuality): FileSize {
        return quality.estimateFileSize(fileSize, bitrate)
    }

    /**
     * Checks if transcoding is required for the given quality
     */
    fun needsTranscoding(quality: AudioQuality): Boolean {
        return quality.isTranscodingRequired(bitrate)
    }

    /**
     * Creates a copy with updated download status
     */
    fun withDownloadStatus(status: DownloadStatus): Song {
        return copy(downloadStatus = status)
    }

    /**
     * Marks the song as downloaded with local path
     */
    fun markAsDownloaded(localPath: String): Song {
        require(localPath.isNotBlank()) { "Local path cannot be blank" }
        return copy(downloadStatus = DownloadStatus.Downloaded(localPath))
    }

    /**
     * Marks the song as downloading with progress
     */
    fun markAsDownloading(progress: Float): Song {
        return copy(downloadStatus = DownloadStatus.Downloading(progress))
    }

    /**
     * Marks the song as not downloaded
     */
    fun markAsNotDownloaded(): Song {
        return copy(downloadStatus = DownloadStatus.NotDownloaded)
    }

    /**
     * Checks if two songs have the same metadata (excluding download status)
     */
    fun hasSameMetadata(other: Song): Boolean {
        return id == other.id &&
                title == other.title &&
                artist == other.artist &&
                album == other.album &&
                duration == other.duration &&
                bitrate == other.bitrate
    }

    /**
     * Returns a formatted string for display
     */
    fun displayName(): String {
        return "$title - ${artistName()}"
    }

    companion object {
        /**
         * Creates a new Song with generated ID
         */
        fun create(
            title: String,
            artist: String? = null,
            album: String? = null,
            duration: Duration,
            filePath: String?,
            uri: String?,
            coverArtUri: String? = null,
            fileSize: FileSize,
            mimeType: String? = null,
            bitrate: Int = 0
        ): Song {
            return Song(
                id = SongId.generate(),
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                filePath = filePath,
                uri = uri,
                coverArtUri = coverArtUri,
                fileSize = fileSize,
                mimeType = mimeType,
                bitrate = bitrate,
                dateAdded = System.currentTimeMillis(),
                downloadStatus = DownloadStatus.NotDownloaded
            )
        }
    }
}
