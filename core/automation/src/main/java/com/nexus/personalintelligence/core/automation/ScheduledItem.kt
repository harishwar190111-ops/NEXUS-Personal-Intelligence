package com.nexus.personalintelligence.core.automation

import com.nexus.personalintelligence.core.remote.RemoteCommand
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class RepeatRule {
    ONCE,
    HOURLY,
    DAILY,
    WEEKLY
}

@Serializable
data class ScheduledItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val command: RemoteCommand? = null,
    val initialTimeMs: Long,
    val repeatRule: RepeatRule = RepeatRule.ONCE,
    val isEnabled: Boolean = true,
    val requiresApproval: Boolean = false,
    val isApproved: Boolean = false,
    val lastRunTimeMs: Long = 0L
) {
    /** Calculates the next execution timestamp based on [currentTimeMs] and [repeatRule]. Returns null if completed. */
    fun calculateNextRun(currentTimeMs: Long): Long? {
        if (!isEnabled) return null
        if (requiresApproval && !isApproved) return null

        if (repeatRule == RepeatRule.ONCE) {
            return if (lastRunTimeMs == 0L) {
                initialTimeMs
            } else {
                null
            }
        }

        val stepMs = when (repeatRule) {
            RepeatRule.HOURLY -> 3600_000L
            RepeatRule.DAILY -> 86400_000L
            RepeatRule.WEEKLY -> 7 * 86400_000L
            RepeatRule.ONCE -> return null
        }

        return if (lastRunTimeMs == 0L) {
            if (initialTimeMs >= currentTimeMs) {
                initialTimeMs
            } else {
                val periods = ((currentTimeMs - initialTimeMs) / stepMs) + 1
                initialTimeMs + periods * stepMs
            }
        } else {
            lastRunTimeMs + stepMs
        }
    }
}

class ScheduledItemStore(
    private val maxItems: Int = 50
) {
    private val items = mutableListOf<ScheduledItem>()

    @Synchronized
    fun addItem(item: ScheduledItem): Boolean {
        if (items.size >= maxItems) return false
        items.add(item)
        return true
    }

    @Synchronized
    fun removeItem(id: String): Boolean {
        return items.removeIf { it.id == id }
    }

    @Synchronized
    fun setEnabled(id: String, enabled: Boolean): Boolean {
        val idx = items.indexOfFirst { it.id == id }
        if (idx == -1) return false
        items[idx] = items[idx].copy(isEnabled = enabled)
        return true
    }

    @Synchronized
    fun approve(id: String): Boolean {
        val idx = items.indexOfFirst { it.id == id }
        if (idx == -1) return false
        items[idx] = items[idx].copy(isApproved = true)
        return true
    }

    @Synchronized
    fun recordExecution(id: String, executionTimeMs: Long): Boolean {
        val idx = items.indexOfFirst { it.id == id }
        if (idx == -1) return false
        val cur = items[idx]
        items[idx] = cur.copy(lastRunTimeMs = executionTimeMs)
        return true
    }

    @Synchronized
    fun getNextDueItems(currentTimeMs: Long): List<ScheduledItem> {
        return items.filter { item ->
            val nextRun = item.calculateNextRun(currentTimeMs)
            nextRun != null && nextRun <= currentTimeMs
        }
    }

    @Synchronized
    fun getAllItems(): List<ScheduledItem> = items.toList()

    @Synchronized
    fun size(): Int = items.size
}
