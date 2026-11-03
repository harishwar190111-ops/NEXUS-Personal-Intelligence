package com.nexus.personalintelligence.core.design

import kotlinx.serialization.Serializable

@Serializable
enum class AnimatedBackgroundStyle {
    FLUID_WAVES, AURORA, PARTICLE_FIELD, GRADIENT_MOTION, LIGHT_WAVES, AMBIENT_GLOW, CINEMATIC_MOTION, CUSTOM_IMAGE
}

@Serializable
enum class BackgroundPerformanceQuality { LOW, BALANCED, HIGH }

@Serializable
data class BackgroundConfig(
    val style: AnimatedBackgroundStyle = AnimatedBackgroundStyle.AURORA,
    val speed: Float = 1.0f,
    val intensity: Float = 1.0f,
    val particleDensity: Int = 50,
    val transparency: Float = 0.85f,
    val brightness: Float = 0.9f,
    val quality: BackgroundPerformanceQuality = BackgroundPerformanceQuality.BALANCED,
    val isEnabled: Boolean = true,
    val customImageUrl: String? = null
)
