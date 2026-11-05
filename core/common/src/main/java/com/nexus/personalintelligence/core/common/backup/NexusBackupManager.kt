package com.nexus.personalintelligence.core.common.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class NexusBackupData(
    val version: Int = 2,
    val backupId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val themePresetId: String = "obsidian",
    val homeLayout: String = "ROWS",
    val favouriteApps: List<String> = emptyList(),
    val customShortcuts: List<String> = emptyList(),
    val wallpaperPath: String = "",
    val use24HourClock: Boolean = true,
    val assistantPreferences: Map<String, String> = emptyMap(),
    val exportedByAppVersion: String = "2.0.0"
)

sealed class RestoreResult {
    data class Success(val backupData: NexusBackupData) : RestoreResult()
    data class Failure(val reason: String) : RestoreResult()
}

class NexusBackupManager(
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) {
    fun exportBackupJson(
        themePresetId: String = "obsidian",
        homeLayout: String = "ROWS",
        favouriteApps: List<String> = emptyList(),
        customShortcuts: List<String> = emptyList(),
        wallpaperPath: String = "",
        use24HourClock: Boolean = true,
        assistantPreferences: Map<String, String> = emptyMap(),
        rawSecretsToScrub: List<String> = emptyList()
    ): String {
        // 1. Sanitize assistant preferences to remove any keys that contain secrets
        val safeAssistantPrefs = assistantPreferences.filterKeys { key ->
            val k = key.lowercase()
            !k.contains("key") &&
                !k.contains("token") &&
                !k.contains("secret") &&
                !k.contains("password") &&
                !k.contains("pin") &&
                !k.contains("auth") &&
                !k.contains("credential")
        }

        val backupData = NexusBackupData(
            themePresetId = themePresetId,
            homeLayout = homeLayout,
            favouriteApps = favouriteApps,
            customShortcuts = customShortcuts,
            wallpaperPath = sanitizeWallpaperReference(wallpaperPath),
            use24HourClock = use24HourClock,
            assistantPreferences = safeAssistantPrefs
        )

        var exportedJson = json.encodeToString(backupData)

        // 2. Redact known secrets passed in
        for (secret in rawSecretsToScrub) {
            if (secret.isNotBlank()) {
                exportedJson = exportedJson.replace(secret, "[REDACTED_SECRET]")
            }
        }

        // 3. Regex scrubbing for common secret patterns
        exportedJson = exportedJson.replace(Regex("(?i)sk-[A-Za-z0-9_-]{10,}"), "[REDACTED_API_KEY]")
        exportedJson = exportedJson.replace(Regex("(?i)AIza[0-9A-Za-z-_]{20,}"), "[REDACTED_API_KEY]")
        exportedJson = exportedJson.replace(Regex("(?i)Bearer\\s+([A-Za-z0-9_.-]+)"), "Bearer [REDACTED_TOKEN]")

        return exportedJson
    }

    fun restoreBackupJson(jsonString: String, currentSupportedVersion: Int = 2): RestoreResult {
        if (jsonString.isBlank()) {
            return RestoreResult.Failure("Empty backup payload")
        }
        return try {
            val parsed = json.decodeFromString<NexusBackupData>(jsonString)
            if (parsed.version <= 0) {
                return RestoreResult.Failure("Invalid backup version: ${parsed.version}")
            }
            if (parsed.version > currentSupportedVersion) {
                return RestoreResult.Failure("Unsupported future backup version: ${parsed.version} (current supported version is $currentSupportedVersion)")
            }
            if (parsed.themePresetId.isBlank()) {
                return RestoreResult.Failure("Missing theme preset ID")
            }
            RestoreResult.Success(parsed)
        } catch (e: Exception) {
            RestoreResult.Failure("Malformed backup JSON: ${e.message}")
        }
    }

    fun sanitizeWallpaperReference(path: String): String {
        // Validate wallpaper reference; return safe fallback if blank or invalid
        val trimmed = path.trim()
        return if (trimmed.startsWith("/") || trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
            trimmed
        } else {
            ""
        }
    }
}
