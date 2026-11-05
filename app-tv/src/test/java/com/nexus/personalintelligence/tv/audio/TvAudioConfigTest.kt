package com.nexus.personalintelligence.tv.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TvAudioConfigTest {

    @Test
    fun testVolumeClamping() {
        val low = TvAudioConfig(volume = -0.5f)
        assertEquals(0.0f, low.clampedVolume, 0.001f)

        val high = TvAudioConfig(volume = 1.5f)
        assertEquals(1.0f, high.clampedVolume, 0.001f)

        val normal = TvAudioConfig(volume = 0.65f)
        assertEquals(0.65f, normal.clampedVolume, 0.001f)
    }

    @Test
    fun testQuietModeSuppressesAllSounds() {
        val quiet = TvAudioConfig(isQuietMode = true)
        for (effect in TvSoundEffect.values()) {
            assertFalse("All effects must be suppressed in quiet mode", quiet.isEffectAllowed(effect))
        }

        val disabled = TvAudioConfig(isEnabled = false)
        for (effect in TvSoundEffect.values()) {
            assertFalse("All effects must be suppressed when audio is disabled", disabled.isEffectAllowed(effect))
        }

        val zeroVol = TvAudioConfig(volume = 0.0f)
        for (effect in TvSoundEffect.values()) {
            assertFalse("All effects must be suppressed at zero volume", zeroVol.isEffectAllowed(effect))
        }
    }

    @Test
    fun testIndividualSoundCategoryToggles() {
        // Startup sounds toggle
        val noStartup = TvAudioConfig(startupSoundEnabled = false)
        assertFalse(noStartup.isEffectAllowed(TvSoundEffect.STARTUP))
        assertFalse(noStartup.isEffectAllowed(TvSoundEffect.LOGO_REVEAL))
        assertTrue(noStartup.isEffectAllowed(TvSoundEffect.FOCUS_MOVE))

        // Navigation sounds toggle
        val noNav = TvAudioConfig(navigationSoundsEnabled = false)
        assertFalse(noNav.isEffectAllowed(TvSoundEffect.FOCUS_MOVE))
        assertFalse(noNav.isEffectAllowed(TvSoundEffect.BACK_NAV))
        assertTrue(noNav.isEffectAllowed(TvSoundEffect.BUTTON_PRESS))

        // Selection sounds toggle
        val noSelect = TvAudioConfig(selectionSoundsEnabled = false)
        assertFalse(noSelect.isEffectAllowed(TvSoundEffect.BUTTON_PRESS))
        assertFalse(noSelect.isEffectAllowed(TvSoundEffect.APP_SELECT))
        assertTrue(noSelect.isEffectAllowed(TvSoundEffect.STARTUP))

        // Connection sounds toggle
        val noConn = TvAudioConfig(connectionSoundsEnabled = false)
        assertFalse(noConn.isEffectAllowed(TvSoundEffect.PAIRING_SUCCESS))
        assertFalse(noConn.isEffectAllowed(TvSoundEffect.CONNECTION_LOST))
        assertTrue(noConn.isEffectAllowed(TvSoundEffect.ERROR))

        // Error sounds toggle
        val noErr = TvAudioConfig(errorSoundsEnabled = false)
        assertFalse(noErr.isEffectAllowed(TvSoundEffect.ERROR))
        assertTrue(noErr.isEffectAllowed(TvSoundEffect.STARTUP))
    }

    @Test
    fun testAudioToneGeneratorSamples() {
        val (freq, durationMs) = AudioToneGenerator.getToneSpec(TvSoundEffect.STARTUP)
        assertEquals(440.0, freq, 0.001)
        assertEquals(500, durationMs)

        val sampleRate = 44100
        val samples = AudioToneGenerator.generateSamples(freq, durationMs, volume = 0.5f, sampleRate = sampleRate)
        val expectedLength = (sampleRate * (durationMs / 1000.0)).toInt()
        assertEquals(expectedLength, samples.size)

        // Verify amplitude bounded by volume * Short.MAX_VALUE
        val maxAllowed = (Short.MAX_VALUE * 0.5f).toInt()
        for (sample in samples) {
            assertTrue("Sample amplitude must be within volume bounds", abs(sample.toInt()) <= maxAllowed + 1)
        }
    }
}
