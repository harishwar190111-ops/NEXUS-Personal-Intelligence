package com.nexus.personalintelligence.core.remote.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsReportTest {

    @Test
    fun testCommandHistoryBoundedCapacity() {
        val tracker = CommandHistoryTracker(maxCapacity = 3)
        assertEquals(0, tracker.size())

        tracker.addEntry(CommandHistoryEntry(commandId = "cmd-1", type = "DPAD_UP", success = true))
        tracker.addEntry(CommandHistoryEntry(commandId = "cmd-2", type = "DPAD_DOWN", success = true))
        tracker.addEntry(CommandHistoryEntry(commandId = "cmd-3", type = "DPAD_LEFT", success = true))
        assertEquals(3, tracker.size())

        // 4th entry exceeds capacity; oldest (cmd-1) should be evicted
        tracker.addEntry(CommandHistoryEntry(commandId = "cmd-4", type = "DPAD_RIGHT", success = true))
        assertEquals(3, tracker.size())

        val history = tracker.getHistory()
        assertEquals("cmd-2", history[0].commandId)
        assertEquals("cmd-3", history[1].commandId)
        assertEquals("cmd-4", history[2].commandId)

        // Clear
        tracker.clearHistory()
        assertEquals(0, tracker.size())
    }

    @Test
    fun testSecretRedactionFromDiagnosticExport() {
        val generator = NexusDiagnosticReportGenerator()

        val rawApiKey = "sk-live-secret-key-1234567890abcdef"
        val rawSessionToken = "session-tok-98765-xyz"
        val rawPin = "482915"

        val report = DiagnosticReport(
            reportId = "rep-001",
            connectionState = "CONNECTED",
            pairingStatus = "APPROVED",
            commandHistory = listOf(
                CommandHistoryEntry(
                    commandId = "c1",
                    type = "AUTH",
                    success = true,
                    message = "Authenticated with token=$rawSessionToken and pin=$rawPin using key=$rawApiKey"
                )
            ),
            systemMetrics = mapOf(
                "token" to rawSessionToken,
                "api_key" to rawApiKey,
                "heap_free" to "14MB"
            )
        )

        val sanitizedJson = generator.generateSanitizedJson(
            report = report,
            knownSecrets = listOf(rawApiKey, rawSessionToken, rawPin)
        )

        // Verify none of the raw secrets appear in the exported JSON
        assertFalse("API key must be redacted", sanitizedJson.contains(rawApiKey))
        assertFalse("Session token must be redacted", sanitizedJson.contains(rawSessionToken))
        assertFalse("PIN must be redacted", sanitizedJson.contains(rawPin))

        // Verify redaction placeholders appear
        assertTrue("Redaction tag should be present", sanitizedJson.contains("[REDACTED"))
        assertTrue("Heap metric should remain intact", sanitizedJson.contains("14MB"))
        assertTrue("Report ID should remain intact", sanitizedJson.contains("rep-001"))
    }
}
