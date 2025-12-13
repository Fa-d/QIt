package dev.sadakat.qit.shared.domain.valueobject

/**
 * Value Object representing metadata about synchronization state
 * Used for delta sync to track what has changed since last sync
 */
data class SyncMetadata(
    val lastSyncTimestamp: Long,
    val syncVersion: Int,
    val pendingChanges: List<ChangeRecord>
) {
    init {
        require(lastSyncTimestamp >= 0) { "Last sync timestamp cannot be negative" }
        require(syncVersion >= 0) { "Sync version cannot be negative" }
    }

    /**
     * Checks if there are pending changes to sync
     */
    fun hasPendingChanges(): Boolean = pendingChanges.isNotEmpty()

    /**
     * Returns the number of pending changes
     */
    fun pendingChangeCount(): Int = pendingChanges.size

    /**
     * Filters pending changes by entity type
     */
    fun pendingChangesByType(entityType: EntityType): List<ChangeRecord> {
        return pendingChanges.filter { it.entityType == entityType }
    }

    /**
     * Filters pending changes by change type
     */
    fun pendingChangesByChangeType(changeType: ChangeType): List<ChangeRecord> {
        return pendingChanges.filter { it.changeType == changeType }
    }

    /**
     * Returns pending changes newer than the given timestamp
     */
    fun changesNewerThan(timestamp: Long): List<ChangeRecord> {
        return pendingChanges.filter { it.timestamp > timestamp }
    }

    /**
     * Adds a new change record to pending changes
     */
    fun addChange(change: ChangeRecord): SyncMetadata {
        // Remove any existing change for the same entity to avoid duplicates
        val filteredChanges = pendingChanges.filterNot { it.isSameEntity(change) }
        return copy(pendingChanges = filteredChanges + change)
    }

    /**
     * Adds multiple change records to pending changes
     */
    fun addChanges(changes: List<ChangeRecord>): SyncMetadata {
        if (changes.isEmpty()) return this

        var updated = this
        changes.forEach { change ->
            updated = updated.addChange(change)
        }
        return updated
    }

    /**
     * Removes changes for the given entity IDs (marks them as synced)
     */
    fun removeChanges(entityIds: List<String>): SyncMetadata {
        val filteredChanges = pendingChanges.filterNot { entityIds.contains(it.entityId) }
        return copy(pendingChanges = filteredChanges)
    }

    /**
     * Updates the last sync timestamp
     */
    fun updateLastSyncTimestamp(timestamp: Long): SyncMetadata {
        require(timestamp >= lastSyncTimestamp) { "New timestamp must be >= current timestamp" }
        return copy(lastSyncTimestamp = timestamp)
    }

    /**
     * Increments the sync version
     */
    fun incrementVersion(): SyncMetadata {
        return copy(syncVersion = syncVersion + 1)
    }

    /**
     * Clears all pending changes
     */
    fun clearPendingChanges(): SyncMetadata {
        return copy(pendingChanges = emptyList())
    }

    /**
     * Updates after a successful sync
     */
    fun markSyncCompleted(timestamp: Long = System.currentTimeMillis()): SyncMetadata {
        return copy(
            lastSyncTimestamp = timestamp,
            syncVersion = syncVersion + 1,
            pendingChanges = emptyList()
        )
    }

    companion object {
        /**
         * Creates initial sync metadata (never synced before)
         */
        fun initial(): SyncMetadata {
            return SyncMetadata(
                lastSyncTimestamp = 0L,
                syncVersion = 0,
                pendingChanges = emptyList()
            )
        }

        /**
         * Creates sync metadata for a fresh sync with no pending changes
         */
        fun create(
            lastSyncTimestamp: Long = System.currentTimeMillis(),
            syncVersion: Int = 1
        ): SyncMetadata {
            return SyncMetadata(
                lastSyncTimestamp = lastSyncTimestamp,
                syncVersion = syncVersion,
                pendingChanges = emptyList()
            )
        }
    }
}
