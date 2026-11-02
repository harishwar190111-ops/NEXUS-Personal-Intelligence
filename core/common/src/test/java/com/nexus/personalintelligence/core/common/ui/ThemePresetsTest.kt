package com.nexus.personalintelligence.core.common.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePresetsTest {
    @Test
    fun hasTenUniquePresets() {
        assertEquals(10, ThemePresets.all.size)
        assertEquals(10, ThemePresets.all.map { it.id }.toSet().size)
        assertEquals(10, ThemePresets.all.map { it.label }.toSet().size)
    }

    @Test
    fun presetsDifferInMoreThanColour() {
        // Shape, focus effect, composition, nav placement, type and aspect together must vary widely.
        val signatures = ThemePresets.all.map {
            listOf(it.style.cornerDp, it.style.focusStyle, it.style.homeLayout, it.style.navPlacement, it.style.titleFont, it.style.tileAspect)
        }.toSet()
        assertTrue("expected at least 9 distinct structural signatures", signatures.size >= 9)
        assertTrue(ThemePresets.all.map { it.style.focusStyle }.toSet().size == FocusStyle.entries.size)
        assertTrue(ThemePresets.all.map { it.style.homeLayout }.toSet().size == HomeLayout.entries.size)
        assertTrue(ThemePresets.all.map { it.background.type }.toSet().size >= 6)
    }

    @Test
    fun unknownIdFallsBackToFirstPreset() {
        assertEquals(ThemePresets.DEFAULT_ID, ThemePresets.byId("nope").id)
        assertEquals(ThemePresets.DEFAULT_ID, ThemePresets.byId(null).id)
    }

    @Test
    fun customStudioAppliesUserValuesOnlyToItself() {
        val custom = CustomTheme(NexusAccent.Rose, NexusBackdrop.Graphite, cornerDp = 40, glassPercent = 25)
        val c = ThemePresets.resolve(ThemePresets.CUSTOM_ID, custom, null)
        assertEquals(NexusAccent.Rose, c.accent)
        assertEquals(40, c.style.cornerDp)
        assertEquals(0.25f, c.style.glassAlpha, 0.0001f)
        val other = ThemePresets.resolve("aurora", custom, null)
        assertEquals(NexusAccent.Emerald, other.accent)
        assertEquals(30, other.style.cornerDp)
    }

    @Test
    fun backgroundOverrideWinsOverPresetDefault() {
        val override = BackgroundSpec(type = BackgroundType.Particles)
        assertEquals(BackgroundType.Particles, ThemePresets.resolve("aurora", CustomTheme(), override).background.type)
        assertEquals(BackgroundType.Aurora, ThemePresets.resolve("aurora", CustomTheme(), null).background.type)
    }

    @Test
    fun backgroundCodecRoundTripsAndRejectsGarbage() {
        val spec = BackgroundSpec(BackgroundType.LightWaves, NexusAccent.Amber, NexusAccent.Rose, 5, 2, 60, AnimQuality.High, false, 4, true)
        assertEquals(spec, BackgroundCodec.decode(BackgroundCodec.encode(spec)))
        assertNull(BackgroundCodec.decode(null))
        assertNull(BackgroundCodec.decode("garbage"))
        assertNull(BackgroundCodec.decode("NotAType|Cyan|Azure|3|3|80|Balanced|true|3|false"))
        val clamped = BackgroundCodec.decode("FluidWaves|Cyan|Azure|99|0|5|Balanced|true|3|false")
        assertNotNull(clamped)
        assertEquals(5, clamped!!.speed)
        assertEquals(1, clamped.intensity)
        assertEquals(30, clamped.brightness)
    }
}
