package com.nexus.personalintelligence.core.common.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Full-screen dark gradient with two soft glows. No blur, so it is cheap on an old TV box. */
@Composable
fun NexusBackground(
    backdrop: NexusBackdrop,
    accent: NexusAccent,
    modifier: Modifier = Modifier,
    spec: BackgroundSpec? = null,
    image: ImageBitmap? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(backdrop.top, backdrop.bottom)))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(accent.color.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.04f),
                        radius = size.maxDimension * 0.65f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(backdrop.glow.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height),
                        radius = size.maxDimension * 0.6f,
                    ),
                )
            },
    ) {
        if (spec != null) AnimatedBackdrop(spec, image)
        content()
    }
}

/** Translucent "liquid glass" surface: faint gradient fill plus a hairline border. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val st = LocalNexusStyle.current
    val s = shape ?: RoundedCornerShape(st.cornerDp.dp)
    val bc = if (borderColor == Color.Unspecified) Color.White.copy(alpha = st.borderAlpha) else borderColor
    Box(
        modifier = modifier
            .clip(s)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = st.glassAlpha), Color.White.copy(alpha = st.glassAlpha * 0.4f))))
            .border(borderWidth, bc, s),
        content = content,
    )
}

/** How finished a feature is. Used so the UI never overstates what exists. */
enum class StatusKind { Implemented, Foundation, Planned, Alert }

@Composable
fun StatusChip(
    text: String,
    kind: StatusKind,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
) {
    val color = when (kind) {
        StatusKind.Implemented -> NexusColors.Ok
        StatusKind.Foundation -> LocalNexusAccent.current.color
        StatusKind.Planned -> NexusColors.TextSecondary
        StatusKind.Alert -> NexusColors.Warn
    }
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = fontSize, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun NexusWordmark(
    modifier: Modifier = Modifier,
    textSize: TextUnit = 20.sp,
    markSize: Dp = 22.dp,
) {
    val accent = LocalNexusAccent.current.color
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(markSize)) {
            val stroke = 2.dp.toPx()
            drawCircle(color = accent, radius = size.minDimension / 2f - stroke, style = Stroke(width = stroke))
            drawCircle(color = accent, radius = size.minDimension / 6f)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "NEXUS",
            fontSize = textSize,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 4.sp,
            color = NexusColors.TextPrimary,
        )
    }
}
