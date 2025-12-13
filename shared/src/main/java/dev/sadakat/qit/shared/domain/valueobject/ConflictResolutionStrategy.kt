package dev.sadakat.qit.shared.domain.valueobject

/**
 * Value Object representing the strategy for resolving synchronization conflicts
 * between phone and watch modifications
 */
enum class ConflictResolutionStrategy {
    /**
     * Most recent timestamp wins the conflict
     * The version with the later updatedAt timestamp is chosen
     */
    LAST_WRITE_WINS,

    /**
     * Phone version always wins conflicts
     * Useful when phone is considered the authoritative source
     */
    PHONE_WINS,

    /**
     * Watch version always wins conflicts
     * Useful when watch modifications should take precedence
     */
    WATCH_WINS,

    /**
     * Requires manual user intervention to resolve
     * Conflict is stored and presented to user for resolution
     */
    MANUAL;

    /**
     * Checks if this strategy requires user intervention
     */
    fun requiresUserIntervention(): Boolean = this == MANUAL

    /**
     * Checks if this strategy can be resolved automatically
     */
    fun isAutomatic(): Boolean = !requiresUserIntervention()

    companion object {
        /**
         * Default strategy for conflict resolution
         */
        val DEFAULT = LAST_WRITE_WINS
    }
}
