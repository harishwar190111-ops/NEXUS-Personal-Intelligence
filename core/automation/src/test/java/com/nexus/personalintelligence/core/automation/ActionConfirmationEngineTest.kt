package com.nexus.personalintelligence.core.automation

import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionConfirmationEngineTest {

    private val engine = ActionConfirmationEngine(defaultTimeoutMs = 10_000L)

    @Test
    fun testNonDestructiveActionAutoConfirmed() {
        val cmd = RemoteCommand(type = RemoteCommandType.DPAD_UP)
        val proposal = engine.proposeAction(cmd, "Navigate up")

        assertFalse("Navigation is not destructive", proposal.isDestructive)
        assertEquals(ConfirmationState.CONFIRMED, proposal.state)
    }

    @Test
    fun testDestructiveActionRequiresConfirmation() {
        val powerCmd = RemoteCommand(type = RemoteCommandType.POWER)
        val powerProposal = engine.proposeAction(powerCmd, "Power off TV")
        assertTrue(powerProposal.isDestructive)
        assertEquals(ConfirmationState.PENDING_USER_CONFIRMATION, powerProposal.state)

        val rebootProposal = engine.proposeAction(
            RemoteCommand(type = RemoteCommandType.HOME),
            "Reboot system and reset network"
        )
        assertTrue(rebootProposal.isDestructive)
        assertEquals(ConfirmationState.PENDING_USER_CONFIRMATION, rebootProposal.state)
    }

    @Test
    fun testUserConfirmationFlow() {
        val t0 = 100_000L
        val proposal = engine.proposeAction(
            RemoteCommand(type = RemoteCommandType.POWER),
            "Turn off device",
            createdAt = t0
        )

        assertEquals(ConfirmationState.PENDING_USER_CONFIRMATION, proposal.state)

        val confirmed = engine.confirmAction(proposal.proposalId, currentTimeMs = t0 + 2000L)
        assertTrue(confirmed)
        assertEquals(ConfirmationState.CONFIRMED, engine.getProposal(proposal.proposalId)?.state)
    }

    @Test
    fun testUserCancellationFlow() {
        val proposal = engine.proposeAction(
            RemoteCommand(type = RemoteCommandType.POWER),
            "Turn off device"
        )

        val cancelled = engine.cancelAction(proposal.proposalId)
        assertTrue(cancelled)
        assertEquals(ConfirmationState.CANCELLED, engine.getProposal(proposal.proposalId)?.state)

        // Cannot confirm a cancelled action
        assertFalse(engine.confirmAction(proposal.proposalId))
    }

    @Test
    fun testTimeoutExpiration() {
        val t0 = 500_000L
        val timeoutMs = 5000L
        val proposal = engine.proposeAction(
            command = RemoteCommand(type = RemoteCommandType.POWER),
            description = "Wipe cache and reboot",
            timeoutMs = timeoutMs,
            createdAt = t0
        )

        // After timeout
        val tAfter = t0 + 6000L
        assertTrue(proposal.isExpired(tAfter))

        // Sweep expired before confirming
        val expired = engine.checkAndExpireTimedOutProposals(tAfter)
        assertEquals(1, expired.size)
        assertEquals(proposal.proposalId, expired[0].proposalId)
        assertEquals(ConfirmationState.TIMED_OUT, engine.getProposal(proposal.proposalId)?.state)

        // Confirming a timed out action fails
        val confirmed = engine.confirmAction(proposal.proposalId, currentTimeMs = tAfter)
        assertFalse("Timed out action cannot be confirmed", confirmed)
        assertEquals(ConfirmationState.TIMED_OUT, engine.getProposal(proposal.proposalId)?.state)
    }
}
