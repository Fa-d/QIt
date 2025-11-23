package dev.sadakat.qit.shared.domain.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Publisher for domain events
 * Allows subscribers to react to domain changes
 */
class DomainEventPublisher {

    private val _events = MutableSharedFlow<DomainEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    suspend fun publish(event: DomainEvent) {
        _events.emit(event)
    }

    suspend fun publishAll(events: List<DomainEvent>) {
        events.forEach { _events.emit(it) }
    }

    companion object {
        @Volatile
        private var instance: DomainEventPublisher? = null

        fun getInstance(): DomainEventPublisher {
            return instance ?: synchronized(this) {
                instance ?: DomainEventPublisher().also { instance = it }
            }
        }
    }
}
