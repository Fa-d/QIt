package dev.sadakat.qit.shared.domain.entity

/**
 * Value Object representing a unique Playlist identifier
 */
data class PlaylistId(val value: String) {
    init {
        require(value.isNotBlank()) { "Playlist ID cannot be blank" }
    }

    override fun toString(): String = value

    companion object {
        fun generate(): PlaylistId {
            return PlaylistId(java.util.UUID.randomUUID().toString())
        }

        fun from(value: String): PlaylistId {
            return PlaylistId(value)
        }
    }
}
