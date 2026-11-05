package com.nexus.personalintelligence.core.remote.recovery

import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionRetryPolicyTest {

    @Test
    fun testExponentialBackoffProgression() {
        val config = RetryConfig(
            initialDelayMs = 1000L,
            maxDelayMs = 5000L,
            multiplier = 2.0,
            maxAttempts = 4
        )
        val policy = ConnectionRetryPolicy(config)
        policy.onDisconnected()
        assertEquals(ConnectionRecoveryStatus.DISCONNECTED, policy.status)

        // Attempt 0: 1000 * 2^0 = 1000
        assertEquals(1000L, policy.nextRetryDelayMs())
        assertEquals(ConnectionRecoveryStatus.RECONNECTING, policy.status)

        // Attempt 1: 1000 * 2^1 = 2000
        assertEquals(2000L, policy.nextRetryDelayMs())

        // Attempt 2: 1000 * 2^2 = 4000
        assertEquals(4000L, policy.nextRetryDelayMs())

        // Attempt 3: 1000 * 2^3 = 8000 -> capped at 5000
        assertEquals(5000L, policy.nextRetryDelayMs())

        // Attempt 4: exhausted
        assertNull(policy.nextRetryDelayMs())
        assertTrue(policy.isExhausted)
        assertEquals(ConnectionRecoveryStatus.EXHAUSTED, policy.status)
    }

    @Test
    fun testResetOnSuccessfulConnection() {
        val policy = ConnectionRetryPolicy(RetryConfig(maxAttempts = 3))
        policy.nextRetryDelayMs()
        policy.nextRetryDelayMs()
        assertEquals(2, policy.attempts)

        policy.onConnected()
        assertEquals(0, policy.attempts)
        assertFalse(policy.isExhausted)
        assertEquals(ConnectionRecoveryStatus.CONNECTED, policy.status)
    }

    @Test
    fun testDestructiveCommandsNeverRepeated() {
        val policy = ConnectionRetryPolicy()
        val powerCmd = RemoteCommand(type = RemoteCommandType.POWER)
        assertFalse("Destructive POWER command must never be automatically retried", policy.canRetryCommand(powerCmd))

        val navCmd = RemoteCommand(type = RemoteCommandType.DPAD_CENTER)
        assertTrue("Navigation commands can be safely retried", policy.canRetryCommand(navCmd))

        val volumeCmd = RemoteCommand(type = RemoteCommandType.VOLUME_UP)
        assertTrue("Volume commands can be safely retried", policy.canRetryCommand(volumeCmd))
    }

    @Test
    fun testFilterSafePendingCommandsPreservesNonDestructive() {
        val policy = ConnectionRetryPolicy()
        val pending = listOf(
            RemoteCommand(type = RemoteCommandType.DPAD_UP),
            RemoteCommand(type = RemoteCommandType.POWER),
            RemoteCommand(type = RemoteCommandType.VOLUME_DOWN),
            RemoteCommand(type = RemoteCommandType.POWER)
        )

        val filtered = policy.filterSafePendingCommands(pending)
        assertEquals(2, filtered.size)
        assertEquals(RemoteCommandType.DPAD_UP, filtered[0].type)
        assertEquals(RemoteCommandType.VOLUME_DOWN, filtered[1].type)
    }
}
