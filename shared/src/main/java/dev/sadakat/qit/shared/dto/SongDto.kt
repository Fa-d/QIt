package dev.sadakat.qit.shared.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Song
 * Used for serialization over network (Phone-Watch communication)
 */
@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L,
    val filePath: String? = null,
    val uri: String? = null,
    val coverArtUri: String? = null,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = null,
    val bitrate: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
)
