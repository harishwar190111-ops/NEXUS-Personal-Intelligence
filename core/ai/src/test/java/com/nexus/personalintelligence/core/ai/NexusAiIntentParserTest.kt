package com.nexus.personalintelligence.core.ai

import org.junit.Assert.*
import org.junit.Test

class NexusAiIntentParserTest {
    private val parser = NexusAiIntentParser()

    @Test
    fun testAppLaunchIntent() {
        val intent = parser.parse("Open YouTube")
        assertEquals(IntentAction.OPEN_APP, intent.action)
        assertEquals("com.google.android.youtube.tv", intent.parameter)
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
    }
}
