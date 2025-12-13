package dev.sadakat.qit.shared.domain.valueobject

/**
 * Enum representing the type of change that occurred
 */
enum class ChangeType {
    CREATED,
    UPDATED,
    DELETED
}

/**
 * Value Object representing a record of change for delta synchronization
 * Tracks what changed, when it changed, and what type of change occurred
 */
data class ChangeRecord(
    val entityId: String,
    val entityType: EntityType,
    val changeType: ChangeType,
    val timestamp: Long
) {
    init {
        require(entityId.isNotBlank()) { "Entity ID cannot be blank" }
        require(timestamp > 0) { "Timestamp must be positive" }
    }

    /**
     * Checks if this change is newer than the given timestamp
     */
    fun isNewerThan(otherTimestamp: Long): Boolean {
        return timestamp > otherTimestamp
    }

    /**
     * Checks if this change is for the same entity as another change
     */
    fun isSameEntity(other: ChangeRecord): Boolean {
        return entityId == other.entityId && entityType == other.entityType
    }

    /**
     * Returns a formatted string for display
     */
    fun displayName(): String {
        return "${changeType.name} ${entityType.name} (ID: $entityId) at $timestamp"
    }

    companion object {
        /**
         * Creates a change record for a created entity
         */
        fun created(entityId: String, entityType: EntityType, timestamp: Long = System.currentTimeMillis()): ChangeRecord {
            return ChangeRecord(entityId, entityType, ChangeType.CREATED, timestamp)
        }

        /**
         * Creates a change record for an updated entity
         */
        fun updated(entityId: String, entityType: EntityType, timestamp: Long = System.currentTimeMillis()): ChangeRecord {
            return ChangeRecord(entityId, entityType, ChangeType.UPDATED, timestamp)
        }

        /**
         * Creates a change record for a deleted entity
         */
        fun deleted(entityId: String, entityType: EntityType, timestamp: Long = System.currentTimeMillis()): ChangeRecord {
            return ChangeRecord(entityId, entityType, ChangeType.DELETED, timestamp)
        }
    }
}
