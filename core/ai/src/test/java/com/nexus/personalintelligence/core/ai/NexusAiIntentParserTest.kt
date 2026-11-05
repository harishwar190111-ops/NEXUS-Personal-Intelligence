package com.nexus.personalintelligence.core.ai

import com.nexus.personalintelligence.core.common.NexusResult
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NexusAiIntentParserTest {
    private val parser = NexusAiIntentParser()
    private val engine = NexusAiEngine()

    @Test
    fun testAppLaunchIntent() {
        val intent = parser.parse("Open YouTube")
        assertEquals(IntentAction.OPEN_APP, intent.action)
        assertEquals("com.google.android.youtube.tv", intent.parameter)

        val cmd = parser.toRemoteCommand(intent)
        assertNotNull(cmd)
        assertEquals(RemoteCommandType.LAUNCH_APP, cmd?.type)
        assertEquals("com.google.android.youtube.tv", cmd?.packageName)
    }

    @Test
    fun testThemeChangeIntent() {
        val intent = parser.parse("Change theme to Aurora")
        assertEquals(IntentAction.CHANGE_THEME, intent.action)
        assertEquals("Aurora", intent.parameter)
    }

    @Test
    fun testVolumeIntent() {
        val intent = parser.parse("volume ethu")
        assertEquals(IntentAction.VOLUME_CONTROL, intent.action)
        assertEquals("UP", intent.parameter)

        val cmd = parser.toRemoteCommand(intent)
        assertNotNull(cmd)
        assertEquals(RemoteCommandType.VOLUME_UP, cmd?.type)
    }

    @Test
    fun testTamilVoiceCommands() {
        val homeIntent = parser.parse("முகப்பு")
        assertEquals(IntentAction.NAVIGATE, homeIntent.action)
        val homeCmd = parser.toRemoteCommand(homeIntent)
        assertEquals(RemoteCommandType.HOME, homeCmd?.type)

        val volDownIntent = parser.parse("சத்தத்தை குறை")
        assertEquals(IntentAction.VOLUME_CONTROL, volDownIntent.action)
        assertEquals("DOWN", volDownIntent.parameter)
        val volDownCmd = parser.toRemoteCommand(volDownIntent)
        assertEquals(RemoteCommandType.VOLUME_DOWN, volDownCmd?.type)

        val mediaIntent = parser.parse("பாடலை நிறுத்து")
        assertEquals(IntentAction.MEDIA_CONTROL, mediaIntent.action)
        val mediaCmd = parser.toRemoteCommand(mediaIntent)
        assertEquals(RemoteCommandType.PLAY_PAUSE, mediaCmd?.type)
    }

    @Test
    fun testSecretRedactionInLogsAndErrors() {
        val secretKey = "sk-1234567890abcdef1234567890abcdef"
        val errorWithKey = "Failed call to https://api.openai.com/v1?key=$secretKey with header Bearer $secretKey"
        val redacted = engine.redactSecrets(errorWithKey, secretKey)

        assertFalse("API key must not appear in redacted message", redacted.contains(secretKey))
        assertTrue("API key query param must be redacted", redacted.contains("key=[REDACTED]"))
        assertTrue("Bearer auth token must be redacted", redacted.contains("Bearer [REDACTED]"))
    }

    @Test
    fun testBlankApiKeyConnectionTest() = runBlocking {
        val res = engine.testConnection(AiProviderConfig(apiKey = ""))
        assertTrue("Blank key must fail connection test", res is NexusResult.Error)
        assertEquals("API key is blank. Please enter a valid API key.", (res as NexusResult.Error).message)
    }
}
