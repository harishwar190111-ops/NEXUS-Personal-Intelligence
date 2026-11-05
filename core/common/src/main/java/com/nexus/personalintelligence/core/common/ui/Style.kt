package com.nexus.personalintelligence.core.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

enum class FocusStyle { Ring, Glow, Lift, Underline }
enum class HomeLayout { Rows, Spotlight, Compact }
enum class NavPlacement { Bottom, Top }
enum class TitleFont { Sans, Light, Serif, Mono }

/** Everything a preset can change besides colour: shape, spacing, type, focus, composition, navigation. */
data class NexusStyle(
    val cornerDp: Int = 20,
    val glassAlpha: Float = 0.10f,
    val borderAlpha: Float = 0.14f,
    val tileGapDp: Int = 16,
    val tileWidthScale: Float = 1f,
    val tileAspect: Float = 16f / 9f,
    val focusScale: Float = 1.08f,
    val focusStyle: FocusStyle = FocusStyle.Ring,
    val homeLayout: HomeLayout = HomeLayout.Rows,
    val navPlacement: NavPlacement = NavPlacement.Bottom,
    val titleFont: TitleFont = TitleFont.Sans,
    val labelCaps: Boolean = false,
)

fun TitleFont.family(): FontFamily = when (this) {
    TitleFont.Sans, TitleFont.Light -> FontFamily.SansSerif
    TitleFont.Serif -> FontFamily.Serif
    TitleFont.Mono -> FontFamily.Monospace
}

fun TitleFont.weight(): FontWeight = when (this) {
    TitleFont.Light -> FontWeight.Light
    TitleFont.Serif -> FontWeight.Normal
    TitleFont.Sans, TitleFont.Mono -> FontWeight.Medium
}

/** Text style for headings/clock/greeting, following the active preset's typography. */
@Composable
fun nexusTitleStyle(size: TextUnit): TextStyle {
    val font = LocalNexusStyle.current.titleFont
    return TextStyle(fontFamily = font.family(), fontWeight = font.weight(), fontSize = size)
}

data class ThemePreset(
    val id: String,
    val label: String,
    val blurb: String,
    val accent: NexusAccent,
    val backdrop: NexusBackdrop,
    val style: NexusStyle,
    val background: BackgroundSpec,
)

/** User-editable values that only apply to the "Custom Studio" preset. */
data class CustomTheme(
    val accent: NexusAccent = NexusAccent.Cyan,
    val backdrop: NexusBackdrop = NexusBackdrop.Midnight,
    val cornerDp: Int = 18,
    val glassPercent: Int = 10,
)

data class ResolvedTheme(
    val preset: ThemePreset,
    val accent: NexusAccent,
    val backdrop: NexusBackdrop,
    val style: NexusStyle,
    val background: BackgroundSpec,
)

object ThemePresets {
    const val DEFAULT_ID = "liquid_glass"
    const val CUSTOM_ID = "custom_studio"

    val all: List<ThemePreset> = listOf(
        ThemePreset(
            "liquid_glass", "Liquid Glass", "Frosted translucent cards over slow blue waves.",
            NexusAccent.Cyan, NexusBackdrop.Midnight,
            NexusStyle(cornerDp = 22, glassAlpha = 0.11f, borderAlpha = 0.16f, tileGapDp = 16, focusStyle = FocusStyle.Ring),
            BackgroundSpec(BackgroundType.FluidWaves, NexusAccent.Cyan, NexusAccent.Azure, speed = 3, intensity = 3),
        ),
        ThemePreset(
            "aurora", "Aurora", "Soft, rounded, airy. Glowing focus over drifting aurora light.",
            NexusAccent.Emerald, NexusBackdrop.Aurora,
            NexusStyle(cornerDp = 30, glassAlpha = 0.07f, borderAlpha = 0.10f, tileGapDp = 20, focusScale = 1.06f, focusStyle = FocusStyle.Glow, titleFont = TitleFont.Light),
            BackgroundSpec(BackgroundType.Aurora, NexusAccent.Emerald, NexusAccent.Cyan, speed = 2, intensity = 4),
        ),
        ThemePreset(
            "gradient_glass", "Gradient Glass", "Dense 4:3 cards, strong glass, top navigation, lifting focus.",
            NexusAccent.Violet, NexusBackdrop.Midnight,
            NexusStyle(cornerDp = 16, glassAlpha = 0.18f, borderAlpha = 0.22f, tileGapDp = 14, tileWidthScale = 0.85f, tileAspect = 4f / 3f, focusScale = 1.10f, focusStyle = FocusStyle.Lift, homeLayout = HomeLayout.Compact, navPlacement = NavPlacement.Top),
            BackgroundSpec(BackgroundType.FlowingGradient, NexusAccent.Violet, NexusAccent.Magenta, speed = 3, intensity = 4),
        ),
        ThemePreset(
            "minimal_dark", "Minimal Dark", "Square corners, wide spacing, quiet underline focus, static background.",
            NexusAccent.Ice, NexusBackdrop.Graphite,
            NexusStyle(cornerDp = 6, glassAlpha = 0.04f, borderAlpha = 0.08f, tileGapDp = 24, tileWidthScale = 0.9f, focusScale = 1.04f, focusStyle = FocusStyle.Underline, homeLayout = HomeLayout.Compact),
            BackgroundSpec(BackgroundType.LayeredGradients, NexusAccent.Ice, NexusAccent.Azure, intensity = 1, brightness = 70, animate = false),
        ),
        ThemePreset(
            "holographic", "Holographic", "Large spotlight tiles, capital labels, shifting holographic colour.",
            NexusAccent.Magenta, NexusBackdrop.Midnight,
            NexusStyle(cornerDp = 20, glassAlpha = 0.15f, borderAlpha = 0.30f, tileGapDp = 18, tileAspect = 1.5f, focusStyle = FocusStyle.Glow, homeLayout = HomeLayout.Spotlight, navPlacement = NavPlacement.Top, labelCaps = true),
            BackgroundSpec(BackgroundType.ColourDrift, NexusAccent.Magenta, NexusAccent.Cyan, speed = 3, intensity = 4),
        ),
        ThemePreset(
            "cinematic", "Cinematic", "Wide 21:9 poster cards, serif type, slow warm colour movement.",
            NexusAccent.Amber, NexusBackdrop.Graphite,
            NexusStyle(cornerDp = 3, glassAlpha = 0.05f, borderAlpha = 0.08f, tileGapDp = 22, tileWidthScale = 1.1f, tileAspect = 21f / 9f, focusScale = 1.05f, focusStyle = FocusStyle.Ring, homeLayout = HomeLayout.Spotlight, titleFont = TitleFont.Serif, labelCaps = true),
            BackgroundSpec(BackgroundType.ColourDrift, NexusAccent.Amber, NexusAccent.Rose, speed = 1, intensity = 2, brightness = 70),
        ),
        ThemePreset(
            "neon_future", "Neon Future", "Hard neon outlines, monospace type, compact grid, floating particles.",
            NexusAccent.Lime, NexusBackdrop.Midnight,
            NexusStyle(cornerDp = 10, glassAlpha = 0.06f, borderAlpha = 0.55f, tileGapDp = 14, tileWidthScale = 0.9f, focusScale = 1.10f, focusStyle = FocusStyle.Ring, homeLayout = HomeLayout.Compact, navPlacement = NavPlacement.Top, titleFont = TitleFont.Mono, labelCaps = true),
            BackgroundSpec(BackgroundType.Particles, NexusAccent.Lime, NexusAccent.Cyan, speed = 3, intensity = 4, density = 4),
        ),
        ThemePreset(
            "ambient_flow", "Ambient Flow", "Very round, light type, glowing focus over thin flowing light lines.",
            NexusAccent.Azure, NexusBackdrop.Aurora,
            NexusStyle(cornerDp = 34, glassAlpha = 0.08f, borderAlpha = 0.12f, tileGapDp = 18, tileWidthScale = 1.05f, focusScale = 1.06f, focusStyle = FocusStyle.Glow, titleFont = TitleFont.Light),
            BackgroundSpec(BackgroundType.LightWaves, NexusAccent.Azure, NexusAccent.Cyan, speed = 2, intensity = 3),
        ),
        ThemePreset(
            "modern_minimal", "Modern Minimal", "Soft 7:5 cards, top navigation, lifting focus, still pastel layers.",
            NexusAccent.Rose, NexusBackdrop.Midnight,
            NexusStyle(cornerDp = 14, glassAlpha = 0.05f, borderAlpha = 0.12f, tileGapDp = 20, tileAspect = 1.4f, focusScale = 1.05f, focusStyle = FocusStyle.Lift, homeLayout = HomeLayout.Compact, navPlacement = NavPlacement.Top),
            BackgroundSpec(BackgroundType.LayeredGradients, NexusAccent.Rose, NexusAccent.Violet, intensity = 2, brightness = 80, animate = false),
        ),
        ThemePreset(
            CUSTOM_ID, "Custom Studio", "Your own accent, backdrop, corner radius and glass strength.",
            NexusAccent.Cyan, NexusBackdrop.Midnight,
            NexusStyle(cornerDp = 18, glassAlpha = 0.10f),
            BackgroundSpec(BackgroundType.FluidWaves, NexusAccent.Cyan, NexusAccent.Violet),
        ),
    )

    fun byId(id: String?): ThemePreset = all.firstOrNull { it.id == id } ?: all.first()

    fun isValidId(id: String?): Boolean = all.any { it.id == id }

    fun resolve(presetId: String?, custom: CustomTheme, backgroundOverride: BackgroundSpec?): ResolvedTheme {
        val preset = byId(presetId)
        return if (preset.id == CUSTOM_ID) {
            ResolvedTheme(
                preset, custom.accent, custom.backdrop,
                preset.style.copy(cornerDp = custom.cornerDp, glassAlpha = custom.glassPercent / 100f),
                backgroundOverride ?: preset.background,
            )
        } else {
            ResolvedTheme(preset, preset.accent, preset.backdrop, preset.style, backgroundOverride ?: preset.background)
        }
    }
}
