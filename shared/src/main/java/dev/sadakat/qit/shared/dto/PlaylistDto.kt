package dev.sadakat.qit.shared.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Playlist
 * Used for serialization over network (Phone-Watch communication)
 */
@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val coverArtUri: String? = null
)
