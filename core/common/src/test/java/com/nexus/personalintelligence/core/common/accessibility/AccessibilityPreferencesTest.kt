package com.nexus.personalintelligence.core.common.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityPreferencesTest {

    @Test
    fun testDefaultValues() {
        val config = AccessibilityConfig()
        assertFalse(config.reducedMotionEnabled)
        assertFalse(config.largeTextEnabled)
        assertFalse(config.highContrastEnabled)
        assertEquals(1.0f, config.effectiveFontScale(), 0.001f)
        assertEquals(300L, config.effectiveAnimationDurationMs(300L))
    }

    @Test
    fun testReducedMotionSetsDurationToZero() {
        val manager = AccessibilityManager()
        manager.setReducedMotion(true)

        assertTrue(manager.config.reducedMotionEnabled)
        assertEquals("Animations must be completely bypassed when reduced motion is on",
            0L, manager.config.effectiveAnimationDurationMs(500L))
        assertEquals(0L, manager.config.effectiveAnimationDurationMs(1000L))
    }

    @Test
    fun testAnimationSpeedMultiplierScaling() {
        val manager = AccessibilityManager()
        manager.setAnimationSpeedMultiplier(1.5f)

        assertEquals(450L, manager.config.effectiveAnimationDurationMs(300L))

        manager.setAnimationSpeedMultiplier(0.5f)
        assertEquals(150L, manager.config.effectiveAnimationDurationMs(300L))
    }

    @Test
    fun testLargeTextFontScaling() {
        val manager = AccessibilityManager()
        manager.setLargeText(enabled = true, scale = 1.4f)

        assertTrue(manager.config.largeTextEnabled)
        assertEquals(1.4f, manager.config.effectiveFontScale(), 0.001f)

        // Test clamping
        manager.setLargeText(enabled = true, scale = 3.0f)
        assertEquals(1.8f, manager.config.effectiveFontScale(), 0.001f)

        manager.setLargeText(enabled = false)
        assertEquals(1.0f, manager.config.effectiveFontScale(), 0.001f)
    }

    @Test
    fun testHighContrastAndFocusIndicators() {
        val manager = AccessibilityManager()
        manager.setHighContrast(true)
        manager.setEnhancedFocusIndicators(true)
        manager.setSoundEffects(false)

        assertTrue(manager.config.highContrastEnabled)
        assertTrue(manager.config.enhancedFocusIndicatorsEnabled)
        assertFalse(manager.config.soundEffectsEnabled)

        manager.resetToDefaults()
        assertFalse(manager.config.highContrastEnabled)
        assertTrue(manager.config.soundEffectsEnabled)
    }
}
