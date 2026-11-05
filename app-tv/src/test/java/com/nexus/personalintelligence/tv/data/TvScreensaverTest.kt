package com.nexus.personalintelligence.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvScreensaverTest {

    @Test
    fun testIdleTimeoutTriggersScreensaver() {
        val manager = TvScreensaverManager(timeoutMs = 10_000L)
        val startTime = 100_000L
        manager.recordActivity(startTime)

        assertFalse("Should not be active before timeout", manager.isScreensaverActive.value)

        // After 5 seconds: not idle
        val idle5s = manager.checkIdle(startTime + 5_000L)
        assertFalse("Should not trigger at 5s (threshold is 10s)", idle5s)
        assertFalse(manager.isScreensaverActive.value)

        // After 10 seconds: idle threshold met
        val idle10s = manager.checkIdle(startTime + 10_000L)
        assertTrue("Should trigger at 10s", idle10s)
        assertTrue("Screensaver state must be active", manager.isScreensaverActive.value)
    }

    @Test
    fun testUserActivityWakesScreensaver() {
        val manager = TvScreensaverManager(timeoutMs = 5_000L)
        manager.startScreensaver()
        assertTrue("Screensaver should be active", manager.isScreensaverActive.value)

        // Remote button press or D-pad interaction
        manager.recordActivity()
        assertFalse("User activity must immediately wake/dismiss screensaver", manager.isScreensaverActive.value)
    }

    @Test
    fun testQuickSettingsLifecycle() {
        val manager = TvScreensaverManager()
        assertFalse(manager.isQuickSettingsOpen.value)

        manager.openQuickSettings()
        assertTrue("Quick settings should be open", manager.isQuickSettingsOpen.value)

        manager.closeQuickSettings()
        assertFalse("Quick settings should be closed", manager.isQuickSettingsOpen.value)
    }

    @Test
    fun testDisabledScreensaverTimeout() {
        val manager = TvScreensaverManager(timeoutMs = 0L)
        val startTime = 50_000L
        manager.recordActivity(startTime)

        val triggered = manager.checkIdle(startTime + 1_000_000L)
        assertFalse("Disabled screensaver (0ms) must never auto-trigger", triggered)
        assertFalse(manager.isScreensaverActive.value)
    }
}
