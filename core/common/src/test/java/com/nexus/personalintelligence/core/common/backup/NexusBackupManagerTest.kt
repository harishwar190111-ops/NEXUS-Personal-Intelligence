package com.nexus.personalintelligence.core.common.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NexusBackupManagerTest {

    @Test
    fun testExportExcludesApiKeysAndTokens() {
        val manager = NexusBackupManager()

        val rawOpenAiKey = "sk-live-secret-openai-api-key-999"
        val rawGeminiKey = "AIzaSyDummyGeminiKeyTestingSecret123"
        val rawSessionToken = "session-tok-nexus-777-secret"

        val sensitivePreferences = mapOf(
            "voice_language" to "ta-IN",
            "api_key" to rawOpenAiKey,
            "secret_token" to rawSessionToken,
            "user_password" to "mySuperSecret123",
            "wake_word_enabled" to "true"
        )

        val exportedJson = manager.exportBackupJson(
            themePresetId = "emerald_glass",
            homeLayout = "ROWS",
            favouriteApps = listOf("com.google.android.youtube"),
            customShortcuts = listOf("https://nexus.local"),
            wallpaperPath = "/data/user/0/com.nexus.tv/files/wall.png",
            use24HourClock = true,
            assistantPreferences = sensitivePreferences,
            rawSecretsToScrub = listOf(rawOpenAiKey, rawGeminiKey, rawSessionToken)
        )

        // Verify secrets are NOT present in the exported backup
        assertFalse("OpenAI API key must be excluded", exportedJson.contains(rawOpenAiKey))
        assertFalse("Gemini API key must be excluded", exportedJson.contains(rawGeminiKey))
        assertFalse("Session token must be excluded", exportedJson.contains(rawSessionToken))
        assertFalse("Password key must be filtered from assistant preferences", exportedJson.contains("user_password"))
        assertFalse("API key name must be filtered from assistant preferences", exportedJson.contains("api_key"))

        // Verify safe preferences remain intact
        assertTrue("Safe voice language must be retained", exportedJson.contains("ta-IN"))
        assertTrue("Safe wake word preference must be retained", exportedJson.contains("wake_word_enabled"))
        assertTrue("Theme preset must be retained", exportedJson.contains("emerald_glass"))
        assertTrue("YouTube app must be retained", exportedJson.contains("com.google.android.youtube"))
    }

    @Test
    fun testExportAndRestoreRoundTrip() {
        val manager = NexusBackupManager()

        val exportedJson = manager.exportBackupJson(
            themePresetId = "cyber_amber",
            homeLayout = "SPOTLIGHT",
            favouriteApps = listOf("com.android.chrome", "com.google.android.youtube"),
            customShortcuts = listOf("https://news.google.com"),
            wallpaperPath = "/data/local/wallpaper.jpg",
            use24HourClock = false,
            assistantPreferences = mapOf("preferred_voice" to "female_tamil")
        )

        val restoreResult = manager.restoreBackupJson(exportedJson)
        assertTrue("Restoration must succeed", restoreResult is RestoreResult.Success)

        val restoredData = (restoreResult as RestoreResult.Success).backupData
        assertEquals("cyber_amber", restoredData.themePresetId)
        assertEquals("SPOTLIGHT", restoredData.homeLayout)
        assertEquals(2, restoredData.favouriteApps.size)
        assertEquals("com.android.chrome", restoredData.favouriteApps[0])
        assertEquals("/data/local/wallpaper.jpg", restoredData.wallpaperPath)
        assertFalse(restoredData.use24HourClock)
        assertEquals("female_tamil", restoredData.assistantPreferences["preferred_voice"])
    }

    @Test
    fun testRestoreRejectsMalformedJson() {
        val manager = NexusBackupManager()

        val emptyResult = manager.restoreBackupJson("")
        assertTrue("Empty JSON must fail", emptyResult is RestoreResult.Failure)

        val corruptResult = manager.restoreBackupJson("{ broken json syntax ]")
        assertTrue("Malformed JSON must fail", corruptResult is RestoreResult.Failure)

        val invalidVersionResult = manager.restoreBackupJson("""{"version": -1, "themePresetId": "obsidian"}""")
        assertTrue("Negative version must fail validation", invalidVersionResult is RestoreResult.Failure)
    }

    @Test
    fun testRestoreRejectsFutureVersion() {
        val manager = NexusBackupManager()
        val futureJson = """{"version": 99, "themePresetId": "obsidian"}"""
        val result = manager.restoreBackupJson(futureJson, currentSupportedVersion = 2)
        assertTrue("Future version must be rejected", result is RestoreResult.Failure)
        assertTrue((result as RestoreResult.Failure).reason.contains("Unsupported future backup version"))
    }

    @Test
    fun testWallpaperReferenceSafety() {
        val manager = NexusBackupManager()

        assertEquals("/valid/path/image.png", manager.sanitizeWallpaperReference("/valid/path/image.png"))
        assertEquals("content://media/external/images/12", manager.sanitizeWallpaperReference("content://media/external/images/12"))
        assertEquals("file:///storage/emulated/0/bg.png", manager.sanitizeWallpaperReference("file:///storage/emulated/0/bg.png"))

        // Unsafe / invalid strings sanitized to empty
        assertEquals("", manager.sanitizeWallpaperReference("javascript:alert(1)"))
        assertEquals("", manager.sanitizeWallpaperReference("   "))
    }
}
