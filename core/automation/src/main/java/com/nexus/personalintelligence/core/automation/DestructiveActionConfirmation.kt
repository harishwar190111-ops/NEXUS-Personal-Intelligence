package com.nexus.personalintelligence.core.automation

import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ConfirmationState {
    PENDING_USER_CONFIRMATION,
    CONFIRMED,
    CANCELLED,
    TIMED_OUT,
    EXECUTED,
    FAILED
}

@Serializable
data class ActionProposal(
    val proposalId: String = UUID.randomUUID().toString(),
    val command: RemoteCommand,
    val description: String,
    val isDestructive: Boolean,
    val confirmationTimeoutMs: Long = 30_000L,
    val createdAt: Long = System.currentTimeMillis(),
    var state: ConfirmationState = if (isDestructive) ConfirmationState.PENDING_USER_CONFIRMATION else ConfirmationState.CONFIRMED
) {
    fun isExpired(currentTimeMs: Long): Boolean {
        return state == ConfirmationState.PENDING_USER_CONFIRMATION && (currentTimeMs - createdAt) > confirmationTimeoutMs
    }
}

class ActionConfirmationEngine(
    private val defaultTimeoutMs: Long = 30_000L
) {
    private val proposals = mutableMapOf<String, ActionProposal>()

    companion object {
        private val DESTRUCTIVE_TYPES = setOf(
            RemoteCommandType.POWER
        )

        fun isActionDestructive(command: RemoteCommand, description: String = ""): Boolean {
            if (DESTRUCTIVE_TYPES.contains(command.type)) return true
            val descLower = description.lowercase()
            return descLower.contains("delete") ||
                descLower.contains("clear") ||
                descLower.contains("reboot") ||
                descLower.contains("shutdown") ||
                descLower.contains("reset") ||
                descLower.contains("wipe") ||
                descLower.contains("revoke") ||
                descLower.contains("remove")
        }
    }

    @Synchronized
    fun proposeAction(
        command: RemoteCommand,
        description: String,
        forceRequireConfirmation: Boolean = false,
        timeoutMs: Long = defaultTimeoutMs,
        createdAt: Long = System.currentTimeMillis()
    ): ActionProposal {
        val destructive = forceRequireConfirmation || isActionDestructive(command, description)
        val proposal = ActionProposal(
            command = command,
            description = description,
            isDestructive = destructive,
            confirmationTimeoutMs = timeoutMs,
            createdAt = createdAt,
            state = if (destructive) ConfirmationState.PENDING_USER_CONFIRMATION else ConfirmationState.CONFIRMED
        )
        proposals[proposal.proposalId] = proposal
        return proposal
    }

    @Synchronized
    fun confirmAction(proposalId: String, currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        val proposal = proposals[proposalId] ?: return false
        if (proposal.isExpired(currentTimeMs)) {
            proposal.state = ConfirmationState.TIMED_OUT
            return false
        }
        if (proposal.state == ConfirmationState.PENDING_USER_CONFIRMATION) {
            proposal.state = ConfirmationState.CONFIRMED
            return true
        }
        return false
    }

    @Synchronized
    fun cancelAction(proposalId: String): Boolean {
        val proposal = proposals[proposalId] ?: return false
        if (proposal.state == ConfirmationState.PENDING_USER_CONFIRMATION) {
            proposal.state = ConfirmationState.CANCELLED
            return true
        }
        return false
    }

    @Synchronized
    fun checkAndExpireTimedOutProposals(currentTimeMs: Long): List<ActionProposal> {
        val expired = mutableListOf<ActionProposal>()
        for (proposal in proposals.values) {
            if (proposal.isExpired(currentTimeMs)) {
                proposal.state = ConfirmationState.TIMED_OUT
                expired.add(proposal)
            }
        }
        return expired
    }

    @Synchronized
    fun getProposal(proposalId: String): ActionProposal? = proposals[proposalId]
}
