package com.nexus.personalintelligence.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.personalintelligence.core.common.ui.LocalNexusAccent
import com.nexus.personalintelligence.core.common.ui.NexusColors

/** Scrolling screen with a title, used by most tabs. */
@Composable
fun ScreenScaffold(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Light)
        if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = NexusColors.TextSecondary)
        content()
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = NexusColors.TextSecondary)
}

@Composable
fun GlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val accent = LocalNexusAccent.current.color
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (enabled) accent.copy(alpha = 0.6f) else NexusColors.GlassBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) NexusColors.TextPrimary else NexusColors.TextMuted,
        )
    }
}

@Composable
fun nexusFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LocalNexusAccent.current.color,
    unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
    cursorColor = LocalNexusAccent.current.color,
    focusedTextColor = NexusColors.TextPrimary,
    unfocusedTextColor = NexusColors.TextPrimary,
    focusedLabelColor = LocalNexusAccent.current.color,
    unfocusedLabelColor = NexusColors.TextSecondary,
    focusedPlaceholderColor = NexusColors.TextMuted,
    unfocusedPlaceholderColor = NexusColors.TextMuted,
)
