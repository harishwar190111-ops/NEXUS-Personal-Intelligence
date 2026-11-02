package com.nexus.personalintelligence.core.common.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Shared NEXUS palette: dark, glassy, cyan/blue accents. Used by both apps. */
object NexusColors {
    val Ink = Color(0xFF05080F)
    val Navy = Color(0xFF0A1220)
    val Deep = Color(0xFF0F1B2E)
    val TextPrimary = Color(0xFFEAF2FF)
    val TextSecondary = Color(0xFF9FB3C8)
    val TextMuted = Color(0xFF6B7F95)
    val Ok = Color(0xFF34D399)
    val Warn = Color(0xFFF59E0B)
    val GlassBorder = Color(0x24FFFFFF)
}

enum class NexusAccent(val label: String, val color: Color) {
    Cyan("Cyan", Color(0xFF22D3EE)),
    Azure("Azure", Color(0xFF3B82F6)),
    Violet("Violet", Color(0xFF8B5CF6)),
    Emerald("Emerald", Color(0xFF34D399)),
    Amber("Amber", Color(0xFFF59E0B)),
    Rose("Rose", Color(0xFFFB7185)),
    Magenta("Magenta", Color(0xFFE879F9)),
    Ice("Ice", Color(0xFFE2E8F0)),
    Lime("Lime", Color(0xFFA3E635));

    companion object {
        fun fromId(id: String?): NexusAccent =
            NexusAccent.entries.firstOrNull { it.name == id } ?: Cyan
    }
}

enum class NexusBackdrop(val label: String, val top: Color, val bottom: Color, val glow: Color) {
    Midnight("Midnight", Color(0xFF070B14), Color(0xFF0B1424), Color(0xFF1D4ED8)),
    Aurora("Aurora", Color(0xFF06101C), Color(0xFF0A2230), Color(0xFF0EA5A4)),
    Graphite("Graphite", Color(0xFF0B0D12), Color(0xFF151922), Color(0xFF64748B));

    companion object {
        fun fromId(id: String?): NexusBackdrop =
            NexusBackdrop.entries.firstOrNull { it.name == id } ?: Midnight
    }
}

val LocalNexusAccent = staticCompositionLocalOf { NexusAccent.Cyan }
val LocalNexusStyle = staticCompositionLocalOf { NexusStyle() }

@Composable
fun NexusTheme(
    accent: NexusAccent = NexusAccent.Cyan,
    style: NexusStyle = NexusStyle(),
    content: @Composable () -> Unit,
) {
    val scheme = darkColorScheme(
        primary = accent.color,
        onPrimary = Color(0xFF021018),
        secondary = accent.color,
        background = NexusColors.Ink,
        onBackground = NexusColors.TextPrimary,
        surface = NexusColors.Navy,
        onSurface = NexusColors.TextPrimary,
        surfaceVariant = NexusColors.Deep,
        onSurfaceVariant = NexusColors.TextSecondary,
        outline = NexusColors.TextMuted,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalNexusAccent provides accent,
            LocalNexusStyle provides style,
            LocalContentColor provides NexusColors.TextPrimary,
            content = content,
        )
    }
}
