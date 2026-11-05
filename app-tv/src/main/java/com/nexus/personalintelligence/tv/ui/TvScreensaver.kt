package com.nexus.personalintelligence.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.personalintelligence.core.common.ui.LocalNexusAccent
import com.nexus.personalintelligence.core.common.ui.NexusColors
import com.nexus.personalintelligence.core.common.ui.nexusTitleStyle
import com.nexus.personalintelligence.core.common.util.NexusTime
import java.time.LocalDateTime

@Composable
fun TvScreensaver(
    use24Hour: Boolean = true,
    reducedMotion: Boolean = false,
    onWake: () -> Unit
) {
    val accent = LocalNexusAccent.current.color
    val now = remember { LocalDateTime.now() }
    val formattedTime = remember(now, use24Hour) {
        NexusTime.formatTime(now.toLocalTime(), use24Hour)
    }
    val formattedDate = remember(now) {
        NexusTime.formatDate(now.toLocalDate())
    }

    val transition = rememberInfiniteTransition(label = "screensaverBreathing")
    val animatedAlpha by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "screensaverAlpha"
    )
    val alpha = if (reducedMotion) 0.9f else animatedAlpha

    // Dismiss screensaver on any back or click action
    BackHandler { onWake() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onWake() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer { this.alpha = alpha }
        ) {
            Text(
                text = formattedTime,
                style = nexusTitleStyle(84.sp),
                fontWeight = FontWeight.Light,
                color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = formattedDate,
                fontSize = 24.sp,
                color = accent,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Press any remote button to wake",
                fontSize = 14.sp,
                color = NexusColors.TextMuted
            )
        }
    }
}
