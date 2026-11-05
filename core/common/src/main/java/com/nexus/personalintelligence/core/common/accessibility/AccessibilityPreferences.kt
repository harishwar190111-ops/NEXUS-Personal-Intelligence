package com.nexus.personalintelligence.core.common.accessibility

import kotlinx.serialization.Serializable

@Serializable
data class AccessibilityConfig(
    val largeTextEnabled: Boolean = false,
    val textScaleMultiplier: Float = 1.0f,
    val highContrastEnabled: Boolean = false,
    val reducedMotionEnabled: Boolean = false,
    val animationSpeedMultiplier: Float = 1.0f,
    val enhancedFocusIndicatorsEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val remoteControlOptimized: Boolean = true
) {
    /**
     * When reduced motion is enabled, effective animation duration is 0ms (animations are skipped).
     * Otherwise, duration is scaled by animationSpeedMultiplier (clamped between 0.25f and 3.0f).
     */
    fun effectiveAnimationDurationMs(baseDurationMs: Long): Long {
        if (reducedMotionEnabled) return 0L
        val multiplier = animationSpeedMultiplier.coerceIn(0.25f, 3.0f)
        return (baseDurationMs * multiplier).toLong()
    }

    /**
     * Computes the effective font scale factor for large text.
     */
    fun effectiveFontScale(): Float {
        return if (largeTextEnabled) {
            textScaleMultiplier.coerceIn(1.2f, 1.8f)
        } else {
            1.0f
        }
    }
}

class AccessibilityManager(
    initialConfig: AccessibilityConfig = AccessibilityConfig()
) {
    var config: AccessibilityConfig = initialConfig
        private set

    fun setReducedMotion(enabled: Boolean) {
        config = config.copy(reducedMotionEnabled = enabled)
    }

    fun setLargeText(enabled: Boolean, scale: Float = 1.3f) {
        config = config.copy(
            largeTextEnabled = enabled,
            textScaleMultiplier = if (enabled) scale.coerceIn(1.2f, 1.8f) else 1.0f
        )
    }

    fun setHighContrast(enabled: Boolean) {
        config = config.copy(highContrastEnabled = enabled)
    }

    fun setAnimationSpeedMultiplier(multiplier: Float) {
        config = config.copy(animationSpeedMultiplier = multiplier.coerceIn(0.25f, 3.0f))
    }

    fun setSoundEffects(enabled: Boolean) {
        config = config.copy(soundEffectsEnabled = enabled)
    }

    fun setEnhancedFocusIndicators(enabled: Boolean) {
        config = config.copy(enhancedFocusIndicatorsEnabled = enabled)
    }

    fun resetToDefaults() {
        config = AccessibilityConfig()
    }
}
