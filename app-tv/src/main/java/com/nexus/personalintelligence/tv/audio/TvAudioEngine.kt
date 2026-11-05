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

class TvAudioEngine(
    var config: TvAudioConfig = TvAudioConfig()
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playSound(effect: TvSoundEffect) {
        if (!config.isEffectAllowed(effect)) return
        scope.launch {
            try {
                val (frequency, durationMs) = AudioToneGenerator.getToneSpec(effect)
                val samples = AudioToneGenerator.generateSamples(frequency, durationMs, config.clampedVolume)

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
                            .setSampleRate(AudioToneGenerator.DEFAULT_SAMPLE_RATE)
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
