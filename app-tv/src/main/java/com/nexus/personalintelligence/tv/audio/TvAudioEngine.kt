package com.nexus.personalintelligence.tv.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class TvSoundEffect {
    STARTUP, LOGO_REVEAL, DASHBOARD_APPEAR, APP_OPEN, APP_SELECT,
    FOCUS_MOVE, BUTTON_PRESS, BACK_NAV, THEME_APPLY, BACKGROUND_CHANGE,
    PAIRING_SUCCESS, CONNECTION_LOST, RECONNECTION, AI_ACTIVATION,
    LISTENING_START, LISTENING_STOP, AI_RESPONSE_COMPLETE, NOTIFICATION,
    ERROR, SHUTDOWN_EXIT
}

class TvAudioEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var isEnabled = true
    private var isQuietMode = false
    private var volume = 0.8f
    private val sampleRate = 44100

    fun playSound(effect: TvSoundEffect) {
        if (!isEnabled || isQuietMode) return
        scope.launch {
            try {
                val (frequency, durationMs) = when (effect) {
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

                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i * frequency / sampleRate
                    val envelope = if (i < numSamples / 8) i.toFloat() / (numSamples / 8) else (numSamples - i).toFloat() / (numSamples * 7 / 8)
                    samples[i] = (sin(angle) * Short.MAX_VALUE * volume * envelope).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
            } catch (_: Exception) {}
        }
    }
}
