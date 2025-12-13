package dev.sadakat.qit.shared.domain.valueobject

/**
 * Value Object representing a synchronization conflict between phone and watch
 * Immutable representation of conflicting entity versions
 */
data class SyncConflict(
    /**
     * ID of the entity that has a conflict
     */
    val entityId: String,

    /**
     * Type of the entity (Playlist, Song, etc.)
     */
    val entityType: EntityType,

    /**
     * Version of the entity from the phone
     */
    val phoneVersion: Any?,

    /**
     * Version of the entity from the watch
     */
    val watchVersion: Any?,

    /**
     * Timestamp when the phone version was last updated
     */
    val phoneTimestamp: Long,

    /**
     * Timestamp when the watch version was last updated
     */
    val watchTimestamp: Long,

    /**
     * The resolved version after applying conflict resolution strategy
     * Null if conflict is not yet resolved
     */
    val resolvedVersion: Any? = null,

    /**
     * Strategy to use for resolving this conflict
     */
    val strategy: ConflictResolutionStrategy
) {

    init {
        require(entityId.isNotBlank()) { "Entity ID cannot be blank" }
        require(phoneTimestamp > 0) { "Phone timestamp must be positive" }
        require(watchTimestamp > 0) { "Watch timestamp must be positive" }
    }

    /**
     * Checks if this conflict has been resolved
     */
    fun isResolved(): Boolean = resolvedVersion != null

    /**
     * Checks if phone version is newer based on timestamp
     */
    fun isPhoneNewer(): Boolean = phoneTimestamp > watchTimestamp

    /**
     * Checks if watch version is newer based on timestamp
     */
    fun isWatchNewer(): Boolean = watchTimestamp > phoneTimestamp

    /**
     * Checks if both versions were updated at the same time
     */
    fun isSameTimestamp(): Boolean = phoneTimestamp == watchTimestamp

    /**
     * Returns the time difference between versions in milliseconds
     */
    fun timeDifference(): Long = kotlin.math.abs(phoneTimestamp - watchTimestamp)

    /**
     * Creates a copy with the resolved version set
     */
    fun withResolvedVersion(resolved: Any): SyncConflict {
        return copy(resolvedVersion = resolved)
    }

    /**
     * Creates a copy with a different resolution strategy
     */
    fun withStrategy(newStrategy: ConflictResolutionStrategy): SyncConflict {
        return copy(strategy = newStrategy, resolvedVersion = null)
    }

    /**
     * Returns a human-readable description of the conflict
     */
    fun describe(): String {
        val timeDesc = when {
            isPhoneNewer() -> "Phone version is ${timeDifference()}ms newer"
            isWatchNewer() -> "Watch version is ${timeDifference()}ms newer"
            else -> "Both versions have the same timestamp"
        }

        val statusDesc = if (isResolved()) "RESOLVED" else "PENDING"

        return "$entityType conflict for ID $entityId - $timeDesc [$statusDesc]"
    }

    companion object {
        /**
         * Creates a new unresolved conflict with default strategy
         */
        fun create(
            entityId: String,
            entityType: EntityType,
            phoneVersion: Any?,
            watchVersion: Any?,
            phoneTimestamp: Long,
            watchTimestamp: Long,
            strategy: ConflictResolutionStrategy = ConflictResolutionStrategy.DEFAULT
        ): SyncConflict {
            return SyncConflict(
                entityId = entityId,
                entityType = entityType,
                phoneVersion = phoneVersion,
                watchVersion = watchVersion,
                phoneTimestamp = phoneTimestamp,
                watchTimestamp = watchTimestamp,
                resolvedVersion = null,
                strategy = strategy
            )
        }
    }
}
