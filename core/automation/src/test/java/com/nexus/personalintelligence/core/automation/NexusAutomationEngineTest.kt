package com.nexus.personalintelligence.core.automation

import com.nexus.personalintelligence.core.common.NexusResult
import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NexusAutomationEngineTest {
    @Test
    fun testAutomationApprovalAndExecution() = runBlocking {
        val engine = NexusAutomationEngine()
        val plan = engine.createPlan(
            description = "Launch YouTube",
            commands = listOf(RemoteCommand(type = RemoteCommandType.LAUNCH_APP, packageName = "com.google.android.youtube.tv")),
            requiresApproval = true
        )
        assertTrue(engine.executePlan(plan.planId) { true } is NexusResult.Error)
        engine.approvePlan(plan.planId)
        assertTrue(engine.executePlan(plan.planId) { true } is NexusResult.Success)
    }
}
