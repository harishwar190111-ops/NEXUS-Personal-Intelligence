package com.nexus.personalintelligence.core.remote.recovery

import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import kotlin.math.min
import kotlin.math.roundToLong

data class RetryConfig(
    val initialDelayMs: Long = 1000L,
    val maxDelayMs: Long = 30000L,
    val multiplier: Double = 2.0,
    val maxAttempts: Int = 5
)

enum class ConnectionRecoveryStatus {
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
    EXHAUSTED,
    FAILED
}

class ConnectionRetryPolicy(
    val config: RetryConfig = RetryConfig()
) {
    private var currentAttempt: Int = 0
    var status: ConnectionRecoveryStatus = ConnectionRecoveryStatus.CONNECTED
        private set

    val attempts: Int
        get() = currentAttempt

    val isExhausted: Boolean
        get() = currentAttempt >= config.maxAttempts

    fun onDisconnected() {
        status = ConnectionRecoveryStatus.DISCONNECTED
    }

    fun onConnected() {
        currentAttempt = 0
        status = ConnectionRecoveryStatus.CONNECTED
    }

    /**
     * Calculates the delay for the next retry attempt using exponential backoff.
     * Returns null if max attempts exceeded.
     */
    fun nextRetryDelayMs(): Long? {
        if (isExhausted) {
            status = ConnectionRecoveryStatus.EXHAUSTED
            return null
        }
        status = ConnectionRecoveryStatus.RECONNECTING
        val factor = Math.pow(config.multiplier, currentAttempt.toDouble())
        val calculatedDelay = (config.initialDelayMs * factor).roundToLong()
        val delay = min(calculatedDelay, config.maxDelayMs)
        currentAttempt++
        return delay
    }

    /**
     * Requirement: Never automatically repeat destructive actions.
     */
    fun canRetryCommand(command: RemoteCommand): Boolean {
        return when (command.type) {
            RemoteCommandType.POWER -> false
            else -> true
        }
    }

    /**
     * Filters a list of pending unacknowledged commands, preserving only safe-to-retry actions.
     */
    fun filterSafePendingCommands(pending: List<RemoteCommand>): List<RemoteCommand> {
        return pending.filter { canRetryCommand(it) }
    }
}
