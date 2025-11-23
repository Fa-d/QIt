package dev.sadakat.qit.shared.domain.event

/**
 * Base interface for all domain events
 */
interface DomainEvent {
    val timestamp: Long get() = System.currentTimeMillis()
    val eventId: String get() = java.util.UUID.randomUUID().toString()
}
