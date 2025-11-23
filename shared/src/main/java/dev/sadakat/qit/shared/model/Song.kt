package dev.sadakat.qit.shared.model

data class Song(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val duration: Long = 0L, // Duration in milliseconds
    val filePath: String? = null, // Path on phone
    val uri: String? = null, // Content URI
    val coverArtUri: String? = null,
    val isDownloadedOnWatch: Boolean = false,
    val watchFilePath: String? = null, // Path on watch if downloaded
    val fileSize: Long = 0L, // File size in bytes
    val mimeType: String? = null,
    val bitrate: Int = 0, // Bitrate in kbps
    val dateAdded: Long = System.currentTimeMillis()
)
