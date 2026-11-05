package com.nexus.personalintelligence.core.remote.events

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class EventSeverity {
    INFO,
    SUCCESS,
    WARNING,
    ALERT
}

@Serializable
enum class EventCategory {
    PAIRING,
    ASSISTANT,
    SYSTEM,
    COMMAND
}

@Serializable
data class NexusEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val severity: EventSeverity = EventSeverity.INFO,
    val category: EventCategory = EventCategory.SYSTEM,
    val timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false
)

class NexusEventStore(
    val maxCapacity: Int = 50
) {
    private val events = mutableListOf<NexusEvent>()

    @Synchronized
    fun addEvent(event: NexusEvent) {
        if (events.size >= maxCapacity) {
            events.removeAt(0) // Evict oldest event to stay bounded
        }
        events.add(event)
    }

    @Synchronized
    fun listEvents(unreadOnly: Boolean = false): List<NexusEvent> {
        val filtered = if (unreadOnly) events.filter { !it.isRead } else events
        return filtered.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun markAsRead(eventId: String): Boolean {
        val found = events.find { it.id == eventId } ?: return false
        found.isRead = true
        return true
    }

    @Synchronized
    fun markAllAsRead() {
        events.forEach { it.isRead = true }
    }

    @Synchronized
    fun clearAll() {
        events.clear()
    }

    @Synchronized
    fun unreadCount(): Int = events.count { !it.isRead }

    @Synchronized
    fun size(): Int = events.size
}
