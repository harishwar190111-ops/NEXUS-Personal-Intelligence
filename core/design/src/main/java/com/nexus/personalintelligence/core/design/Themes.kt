package com.nexus.personalintelligence.core.design

import kotlinx.serialization.Serializable

@Serializable
enum class NexusThemePreset {
    LIQUID_GLASS, AURORA, GRADIENT_GLASS, MINIMAL_DARK, HOLOGRAPHIC,
    CINEMATIC, NEON_FUTURE, AMBIENT_FLOW, MODERN_MINIMAL, CUSTOM_STUDIO
}

@Serializable
data class NexusTheme(
    val id: String,
    val name: String,
    val preset: NexusThemePreset,
    val primaryColor: Long,
    val secondaryColor: Long,
    val surfaceColor: Long,
    val backgroundColor: Long,
    val textColor: Long,
    val cardCornerRadiusDp: Int,
    val cardElevationDp: Int,
    val focusBorderColor: Long,
    val focusBorderWidthDp: Int,
    val typographyScale: Float = 1.0f,
    val isGlassmorphism: Boolean = true
)

object ThemeRegistry {
    val presets: Map<NexusThemePreset, NexusTheme> = mapOf(
        NexusThemePreset.LIQUID_GLASS to NexusTheme("liquid_glass", "Liquid Glass", NexusThemePreset.LIQUID_GLASS, 0xFF4A90E2, 0xFF50E3C2, 0x33FFFFFF, 0xFF0D1B2A, 0xFFFFFFFF, 18, 8, 0xFF00FFFF, 4),
        NexusThemePreset.AURORA to NexusTheme("aurora", "Aurora", NexusThemePreset.AURORA, 0xFF00FF87, 0xFF60EFFF, 0x2600FF87, 0xFF05101E, 0xFFE0FFFF, 24, 12, 0xFF60EFFF, 4),
        NexusThemePreset.GRADIENT_GLASS to NexusTheme("grad_glass", "Gradient Glass", NexusThemePreset.GRADIENT_GLASS, 0xFFFF007F, 0xFF7928CA, 0x2D7928CA, 0xFF0F0C20, 0xFFFFF0F5, 16, 10, 0xFFFF007F, 4),
        NexusThemePreset.MINIMAL_DARK to NexusTheme("min_dark", "Minimal Dark", NexusThemePreset.MINIMAL_DARK, 0xFF9E9E9E, 0xFF616161, 0xFF1E1E1E, 0xFF121212, 0xFFE0E0E0, 8, 2, 0xFFFFFFFF, 2, isGlassmorphism = false),
        NexusThemePreset.HOLOGRAPHIC to NexusTheme("holo", "Holographic", NexusThemePreset.HOLOGRAPHIC, 0xFF00F0FF, 0xFFFF003C, 0x2200F0FF, 0xFF030712, 0xFFE2E8F0, 12, 6, 0xFF00F0FF, 3),
        NexusThemePreset.CINEMATIC to NexusTheme("cine", "Cinematic", NexusThemePreset.CINEMATIC, 0xFFE50914, 0xFFB81D24, 0xFF181818, 0xFF000000, 0xFFFFFFFF, 6, 14, 0xFFE50914, 4, isGlassmorphism = false),
        NexusThemePreset.NEON_FUTURE to NexusTheme("neon", "Neon Future", NexusThemePreset.NEON_FUTURE, 0xFF39FF14, 0xFFFF073A, 0x2A111827, 0xFF090A0F, 0xFFF3F4F6, 20, 10, 0xFF39FF14, 4),
        NexusThemePreset.AMBIENT_FLOW to NexusTheme("ambient", "Ambient Flow", NexusThemePreset.AMBIENT_FLOW, 0xFFFFB347, 0xFFFFCC33, 0x20FFB347, 0xFF1A130B, 0xFFFFF8E7, 22, 8, 0xFFFFCC33, 3),
        NexusThemePreset.MODERN_MINIMAL to NexusTheme("mod_min", "Modern Minimal", NexusThemePreset.MODERN_MINIMAL, 0xFF3B82F6, 0xFF10B981, 0xFF1F2937, 0xFF111827, 0xFFF9FAFB, 14, 4, 0xFF3B82F6, 3, isGlassmorphism = false),
        NexusThemePreset.CUSTOM_STUDIO to NexusTheme("custom", "Custom Studio", NexusThemePreset.CUSTOM_STUDIO, 0xFF8B5CF6, 0xFFEC4899, 0x338B5CF6, 0xFF130E26, 0xFFFAF5FF, 16, 8, 0xFFEC4899, 4)
    )

    fun getTheme(preset: NexusThemePreset): NexusTheme = presets[preset] ?: presets[NexusThemePreset.LIQUID_GLASS]!!
}
