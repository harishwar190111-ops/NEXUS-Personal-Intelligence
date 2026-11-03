package com.nexus.personalintelligence.core.automation

import com.nexus.personalintelligence.core.common.NexusResult
import com.nexus.personalintelligence.core.remote.RemoteCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class AutomationStepStatus { PENDING_APPROVAL, APPROVED, EXECUTING, VERIFIED, FAILED, ROLLED_BACK }

@Serializable
data class AutomationActionPlan(
    val planId: String = UUID.randomUUID().toString(),
    val description: String,
    val commands: List<RemoteCommand>,
    val requiresExplicitApproval: Boolean = true,
    var status: AutomationStepStatus = AutomationStepStatus.PENDING_APPROVAL
)

class NexusAutomationEngine {
    private val _activePlans = MutableStateFlow<List<AutomationActionPlan>>(emptyList())
    val activePlans: StateFlow<List<AutomationActionPlan>> = _activePlans.asStateFlow()
    private val _executionHistory = MutableStateFlow<List<AutomationActionPlan>>(emptyList())
    val executionHistory: StateFlow<List<AutomationActionPlan>> = _executionHistory.asStateFlow()

    fun createPlan(description: String, commands: List<RemoteCommand>, requiresApproval: Boolean = true): AutomationActionPlan {
        val plan = AutomationActionPlan(
            description = description,
            commands = commands,
            requiresExplicitApproval = requiresApproval,
            status = if (requiresApproval) AutomationStepStatus.PENDING_APPROVAL else AutomationStepStatus.APPROVED
        )
        _activePlans.value = _activePlans.value + plan
        return plan
    }

    fun approvePlan(planId: String): Boolean {
        val cur = _activePlans.value.toMutableList()
        val idx = cur.indexOfFirst { it.planId == planId }
        if (idx != -1) {
            cur[idx] = cur[idx].copy(status = AutomationStepStatus.APPROVED)
            _activePlans.value = cur
            return true
        }
        return false
    }

    suspend fun executePlan(planId: String, executor: suspend (RemoteCommand) -> Boolean): NexusResult<String> {
        val plan = _activePlans.value.find { it.planId == planId } ?: return NexusResult.Error("Plan not found")
        if (plan.status != AutomationStepStatus.APPROVED) return NexusResult.Error("Requires approval")
        plan.status = AutomationStepStatus.EXECUTING
        for (cmd in plan.commands) {
            if (!executor(cmd)) {
                plan.status = AutomationStepStatus.FAILED
                return NexusResult.Error("Command failed")
            }
        }
        plan.status = AutomationStepStatus.VERIFIED
        _executionHistory.value = _executionHistory.value + plan
        _activePlans.value = _activePlans.value.filter { it.planId != planId }
        return NexusResult.Success("Executed successfully")
    }
}
