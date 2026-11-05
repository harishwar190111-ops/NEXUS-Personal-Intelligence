package com.nexus.personalintelligence.core.ai

import com.nexus.personalintelligence.core.remote.RemoteCommandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvAssistantDialogueEngineTest {

    private val engine = TvAssistantDialogueEngine()

    @Test
    fun testStorageQuerySpeechResponse() {
        val storageBytes = 4L * 1024L * 1024L * 1024L // 4 GB
        val resp = engine.processVoiceQuery(
            query = "How much storage is available?",
            hasCompatibleTvMic = true,
            availableStorageBytes = storageBytes
        )

        assertTrue(resp.spokenText.contains("4.0 gigabytes available"))
        assertTrue(resp.uiFeedbackMessage.contains("Storage: 4.0 gigabytes"))
    }

    @Test
    fun testSettingsAndAppsQueries() {
        // Open settings
        val settingsResp = engine.processVoiceQuery("Open settings", hasCompatibleTvMic = true)
        assertEquals("Opening settings.", settingsResp.spokenText)
        assertNotNull(settingsResp.commandToExecute)
        assertEquals(RemoteCommandType.SETTINGS, settingsResp.commandToExecute?.type)

        // Show applications
        val appsResp = engine.processVoiceQuery("Show my applications", hasCompatibleTvMic = true)
        assertEquals("Showing your installed TV applications.", appsResp.spokenText)
        assertNotNull(appsResp.commandToExecute)
        assertEquals(RemoteCommandType.MENU, appsResp.commandToExecute?.type)

        // Return home
        val homeResp = engine.processVoiceQuery("Return home", hasCompatibleTvMic = true)
        assertEquals("Returning to home screen.", homeResp.spokenText)
        assertEquals(RemoteCommandType.HOME, homeResp.commandToExecute?.type)
    }

    @Test
    fun testMissingTvMicrophoneFallbackHelp() {
        val resp = engine.processVoiceQuery(
            query = "",
            hasCompatibleTvMic = false
        )

        assertTrue(resp.requiresMicrophoneFallbackHelp)
        assertTrue(resp.spokenText.contains("TV microphone is not available"))
        assertTrue(resp.uiFeedbackMessage.contains("phone to speak"))
    }

    @Test
    fun testAppLaunchThroughAssistant() {
        val resp = engine.processVoiceQuery("Open YouTube", hasCompatibleTvMic = true)
        assertNotNull(resp.commandToExecute)
        assertEquals(RemoteCommandType.LAUNCH_APP, resp.commandToExecute?.type)
        assertEquals("com.google.android.youtube.tv", resp.commandToExecute?.packageName)
    }
}
