package dev.sadakat.qit.wear.presentation.model

/**
 * Represents the sync status in the UI layer
 */
sealed class SyncStatus {
    /**
     * No sync in progress
     */
    object Idle : SyncStatus()

    /**
     * Sync is currently in progress
     */
    object Syncing : SyncStatus()

    /**
     * Sync completed successfully
     * @param timestamp Unix timestamp in milliseconds when sync completed
     * @param itemCount Number of items synced
     */
    data class Success(val timestamp: Long, val itemCount: Int) : SyncStatus()

    /**
     * Sync failed with an error
     * @param message Error message to display
     */
    data class Error(val message: String) : SyncStatus()

    /**
     * Sync completed but found conflicts
     * @param conflictCount Number of conflicts detected
     */
    data class Conflict(val conflictCount: Int) : SyncStatus()
}
