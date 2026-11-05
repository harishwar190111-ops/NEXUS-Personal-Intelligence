package com.nexus.personalintelligence.tv.audio

import kotlin.math.sin

data class TvAudioConfig(
    val isEnabled: Boolean = true,
    val volume: Float = 0.8f,
    val isQuietMode: Boolean = false,
    val navigationSoundsEnabled: Boolean = true,
    val selectionSoundsEnabled: Boolean = true,
    val commandSoundsEnabled: Boolean = true,
    val errorSoundsEnabled: Boolean = true,
    val startupSoundEnabled: Boolean = true,
    val connectionSoundsEnabled: Boolean = true
) {
    val clampedVolume: Float
        get() = volume.coerceIn(0.0f, 1.0f)

    fun isEffectAllowed(effect: TvSoundEffect): Boolean {
        if (!isEnabled || isQuietMode || clampedVolume == 0.0f) return false
        return when (effect) {
            TvSoundEffect.STARTUP, TvSoundEffect.LOGO_REVEAL -> startupSoundEnabled
            TvSoundEffect.FOCUS_MOVE, TvSoundEffect.BACK_NAV -> navigationSoundsEnabled
            TvSoundEffect.APP_SELECT, TvSoundEffect.BUTTON_PRESS, TvSoundEffect.APP_OPEN -> selectionSoundsEnabled
            TvSoundEffect.PAIRING_SUCCESS, TvSoundEffect.CONNECTION_LOST, TvSoundEffect.RECONNECTION -> connectionSoundsEnabled
            TvSoundEffect.ERROR -> errorSoundsEnabled
            else -> true
        }
    }
}

object AudioToneGenerator {
    const val DEFAULT_SAMPLE_RATE = 44100

    fun getToneSpec(effect: TvSoundEffect): Pair<Double, Int> = when (effect) {
        TvSoundEffect.STARTUP -> Pair(440.0, 500)
        TvSoundEffect.LOGO_REVEAL -> Pair(523.25, 300)
        TvSoundEffect.DASHBOARD_APPEAR -> Pair(659.25, 250)
        TvSoundEffect.APP_OPEN -> Pair(587.33, 200)
        TvSoundEffect.APP_SELECT -> Pair(880.0, 150)
        TvSoundEffect.FOCUS_MOVE -> Pair(330.0, 50)
        TvSoundEffect.BUTTON_PRESS -> Pair(440.0, 80)
        TvSoundEffect.BACK_NAV -> Pair(349.23, 120)
        TvSoundEffect.THEME_APPLY -> Pair(698.46, 300)
        TvSoundEffect.BACKGROUND_CHANGE -> Pair(783.99, 300)
        TvSoundEffect.PAIRING_SUCCESS -> Pair(880.0, 400)
        TvSoundEffect.CONNECTION_LOST -> Pair(220.0, 400)
        TvSoundEffect.RECONNECTION -> Pair(440.0, 300)
        TvSoundEffect.AI_ACTIVATION -> Pair(987.77, 200)
        TvSoundEffect.LISTENING_START -> Pair(1046.50, 150)
        TvSoundEffect.LISTENING_STOP -> Pair(523.25, 150)
        TvSoundEffect.AI_RESPONSE_COMPLETE -> Pair(659.25, 250)
        TvSoundEffect.NOTIFICATION -> Pair(830.61, 200)
        TvSoundEffect.ERROR -> Pair(150.0, 350)
        TvSoundEffect.SHUTDOWN_EXIT -> Pair(261.63, 400)
    }

    fun generateSamples(
        frequency: Double,
        durationMs: Int,
        volume: Float,
        sampleRate: Int = DEFAULT_SAMPLE_RATE
    ): ShortArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        val clampedVol = volume.coerceIn(0.0f, 1.0f)
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i * frequency / sampleRate
            val envelope = if (i < numSamples / 8) {
                i.toFloat() / (numSamples / 8)
            } else {
                (numSamples - i).toFloat() / (numSamples * 7 / 8)
            }
            samples[i] = (sin(angle) * Short.MAX_VALUE * clampedVol * envelope).toInt().toShort()
        }
        return samples
    }
}
