package com.nexus.personalintelligence.core.remote

import org.junit.Assert.*
import org.junit.Test

class RemoteModelsTest {
    @Test
    fun testRemoteCommandCreation() {
        val cmd = RemoteCommand(type = RemoteCommandType.DPAD_UP)
        assertNotNull(cmd.commandId)
        assertEquals(RemoteCommandType.DPAD_UP, cmd.type)
        assertTrue(cmd.timestamp > 0)
    }

    @Test
    fun testAppCapabilityRegistry() {
        val registry = ThirdPartyAppCapabilityRegistry()
        val netflixCap = registry.getCapability("com.netflix.ninja")
        assertTrue(netflixCap.supportsDirectIntent)
        assertTrue(netflixCap.requiresAccessibilityForFullNavigation)
        assertNotNull(netflixCap.limitationNote)
    }
}
