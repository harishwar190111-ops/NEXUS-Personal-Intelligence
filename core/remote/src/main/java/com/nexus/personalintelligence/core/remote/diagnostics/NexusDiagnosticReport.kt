package com.nexus.personalintelligence.core.remote.diagnostics

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class CommandHistoryEntry(
    val commandId: String = UUID.randomUUID().toString(),
    val type: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = "",
    val latencyMs: Long = 0L
)

@Serializable
data class DiagnosticReport(
    val reportId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "2.0.0",
    val deviceModel: String = "Android Device",
    val connectionState: String = "DISCONNECTED",
    val pairingStatus: String = "NOT_PAIRED",
    val commandHistory: List<CommandHistoryEntry> = emptyList(),
    val systemMetrics: Map<String, String> = emptyMap()
)

class CommandHistoryTracker(
    private val maxCapacity: Int = 50
) {
    private val history = mutableListOf<CommandHistoryEntry>()

    @Synchronized
    fun addEntry(entry: CommandHistoryEntry) {
        if (history.size >= maxCapacity) {
            history.removeAt(0) // evict oldest
        }
        history.add(entry)
    }

    @Synchronized
    fun getHistory(): List<CommandHistoryEntry> = history.toList()

    @Synchronized
    fun clearHistory() {
        history.clear()
    }

    @Synchronized
    fun size(): Int = history.size
}

class NexusDiagnosticReportGenerator(
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) {
    fun generateSanitizedJson(
        report: DiagnosticReport,
        knownSecrets: List<String> = emptyList()
    ): String {
        val rawJson = json.encodeToString(report)
        return sanitize(rawJson, knownSecrets)
    }

    fun sanitize(input: String, knownSecrets: List<String> = emptyList()): String {
        var clean = input

        // 1. Redact any explicitly known secrets (API keys, session tokens, PINs)
        for (secret in knownSecrets) {
            if (secret.isNotBlank()) {
                clean = clean.replace(secret, "[REDACTED_SECRET]")
            }
        }

        // 2. Redact common API key patterns (OpenAI, Gemini, Bearer tokens)
        clean = clean.replace(Regex("(?i)sk-[A-Za-z0-9_-]{10,}"), "[REDACTED_API_KEY]")
        clean = clean.replace(Regex("(?i)AIza[0-9A-Za-z-_]{20,}"), "[REDACTED_API_KEY]")
        clean = clean.replace(Regex("(?i)Bearer\\s+([A-Za-z0-9_.-]+)"), "Bearer [REDACTED_TOKEN]")

        // 3. Redact pairing PINs (6-digit patterns in PIN/pairing key-value contexts)
        clean = clean.replace(Regex("(?i)(\"?(?:pin|pairing_code|code)\"?\\s*[:=]\\s*\"?)\\d{6}(\"?)"), "$1[REDACTED_PIN]$2")

        // 4. Redact session tokens (UUID or token fields)
        clean = clean.replace(Regex("(?i)(\"?(?:sessionToken|session_token|token|secret|password)\"?\\s*[:=]\\s*\")[^\"]+(\")"), "$1[REDACTED_TOKEN]$2")

        return clean
    }
}
