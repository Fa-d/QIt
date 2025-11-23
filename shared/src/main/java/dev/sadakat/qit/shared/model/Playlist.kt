package dev.sadakat.qit.shared.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val coverArtUri: String? = null
)
