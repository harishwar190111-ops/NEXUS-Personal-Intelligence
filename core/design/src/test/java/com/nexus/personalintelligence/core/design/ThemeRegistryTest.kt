package com.nexus.personalintelligence.core.design

import org.junit.Assert.*
import org.junit.Test

class ThemeRegistryTest {
    @Test
    fun testAllTenDistinctThemesExist() {
        assertEquals(10, NexusThemePreset.values().size)
        assertEquals(10, ThemeRegistry.presets.size)
    }

    @Test
    fun testBackgroundConfigDefaults() {
        val bg = BackgroundConfig()
        assertEquals(AnimatedBackgroundStyle.AURORA, bg.style)
    }
}
