package dev.sadakat.qit.shared.domain.entity

/**
 * Value Object representing a unique Song identifier
 */
data class SongId(val value: String) {
    init {
        require(value.isNotBlank()) { "Song ID cannot be blank" }
    }

    override fun toString(): String = value

    companion object {
        fun generate(): SongId {
            return SongId(java.util.UUID.randomUUID().toString())
        }

        fun from(value: String): SongId {
            return SongId(value)
        }
    }
}
