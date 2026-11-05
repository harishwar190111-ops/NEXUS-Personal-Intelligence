package com.nexus.personalintelligence.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.personalintelligence.core.common.ui.GlassPanel
import com.nexus.personalintelligence.core.common.ui.LocalNexusAccent
import com.nexus.personalintelligence.core.common.ui.NexusColors
import com.nexus.personalintelligence.core.common.ui.nexusTitleStyle
import com.nexus.personalintelligence.tv.network.SecurityGateRequest
import kotlinx.coroutines.delay

@Composable
fun SecurityReviewGateDialog(
    request: SecurityGateRequest,
    onDismiss: () -> Unit
) {
    val accent = LocalNexusAccent.current.color
    val approveFocus = remember { FocusRequester() }

    BackHandler {
        request.onReject()
        onDismiss()
    }

    LaunchedEffect(Unit) {
        delay(100)
        runCatching { approveFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            modifier = Modifier.width(520.dp),
            borderColor = accent.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SECURITY REVIEW GATE",
                    style = nexusTitleStyle(22.sp),
                    letterSpacing = 2.sp,
                    color = accent
                )

                Text(
                    text = "A device on your local network is requesting permission to control this TV.",
                    fontSize = 14.sp,
                    color = NexusColors.TextSecondary,
                    lineHeight = 20.sp
                )

                Column(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = request.clientDeviceName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "IP Address: ${request.clientIp}",
                        fontSize = 13.sp,
                        color = NexusColors.TextMuted
                    )
                }

                Text(
                    text = "PAIRING CODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = NexusColors.TextSecondary
                )

                Text(
                    text = request.pin,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 8.sp,
                    color = accent
                )

                Text(
                    text = "Verify this code matches the prompt on your phone before approving.",
                    fontSize = 13.sp,
                    color = NexusColors.TextMuted
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvButton(
                        text = "Approve",
                        onClick = {
                            request.onApprove()
                            onDismiss()
                        },
                        modifier = Modifier.focusRequester(approveFocus)
                    )

                    TvButton(
                        text = "Reject",
                        onClick = {
                            request.onReject()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
