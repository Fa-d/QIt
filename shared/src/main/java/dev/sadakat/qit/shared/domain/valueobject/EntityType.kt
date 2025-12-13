package dev.sadakat.qit.shared.domain.valueobject

/**
 * Value Object representing the type of entity involved in a sync operation
 */
enum class EntityType {
    /**
     * Represents a Playlist entity
     */
    PLAYLIST,

    /**
     * Represents a Song entity
     */
    SONG,

    /**
     * Represents a Song metadata (without download status)
     */
    SONG_METADATA
}
