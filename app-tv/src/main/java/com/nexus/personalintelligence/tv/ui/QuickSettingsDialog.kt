package com.nexus.personalintelligence.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.personalintelligence.core.common.ui.GlassPanel
import com.nexus.personalintelligence.core.common.ui.LocalNexusAccent
import com.nexus.personalintelligence.core.common.ui.NexusColors
import com.nexus.personalintelligence.core.common.ui.StatusChip
import com.nexus.personalintelligence.core.common.ui.StatusKind
import com.nexus.personalintelligence.core.common.ui.nexusTitleStyle
import com.nexus.personalintelligence.tv.network.TvNetworkManager
import kotlinx.coroutines.delay

@Composable
fun QuickSettingsDialog(
    use24Hour: Boolean,
    launcherMode: Boolean,
    pairedPhoneCount: Int,
    onUse24Hour: (Boolean) -> Unit,
    onLauncherMode: (Boolean) -> Unit,
    onStartScreensaver: () -> Unit,
    onOpenFullSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = LocalNexusAccent.current.color
    val firstFocus = remember { FocusRequester() }
    val ip = remember { TvNetworkManager.getLocalIpAddress() }

    BackHandler { onDismiss() }

    LaunchedEffect(Unit) {
        delay(100)
        runCatching { firstFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            modifier = Modifier.width(540.dp),
            borderColor = accent.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUICK SETTINGS",
                        style = nexusTitleStyle(20.sp),
                        color = accent
                    )
                    StatusChip("TV Ready", StatusKind.Implemented)
                }

                // Network Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Local Network Address", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("$ip:8765", fontSize = 13.sp, color = NexusColors.TextMuted)
                    }
                    StatusChip("Port 8765", StatusKind.Foundation)
                }

                // Clock 24h / 12h toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Clock Format", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TvButton(
                            text = "24h",
                            onClick = { onUse24Hour(true) },
                            selected = use24Hour,
                            horizontalPadding = 16.dp,
                            modifier = Modifier.focusRequester(firstFocus)
                        )
                        TvButton(
                            text = "12h",
                            onClick = { onUse24Hour(false) },
                            selected = !use24Hour,
                            horizontalPadding = 16.dp
                        )
                    }
                }

                // Launcher Mode (Home App)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Launcher Mode", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(if (launcherMode) "Registered as Home" else "Default launcher active", fontSize = 12.sp, color = NexusColors.TextMuted)
                    }
                    TvButton(
                        text = if (launcherMode) "Turn Off" else "Turn On",
                        onClick = { onLauncherMode(!launcherMode) },
                        selected = launcherMode,
                        horizontalPadding = 16.dp
                    )
                }

                // Paired Phones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Paired Phones", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    StatusChip(
                        if (pairedPhoneCount > 0) "$pairedPhoneCount paired" else "None",
                        if (pairedPhoneCount > 0) StatusKind.Implemented else StatusKind.Alert
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TvButton(
                        text = "Screensaver",
                        onClick = {
                            onDismiss()
                            onStartScreensaver()
                        },
                        horizontalPadding = 18.dp
                    )
                    TvButton(
                        text = "Full Settings",
                        onClick = {
                            onDismiss()
                            onOpenFullSettings()
                        },
                        horizontalPadding = 18.dp
                    )
                    TvButton(
                        text = "Close",
                        onClick = onDismiss,
                        horizontalPadding = 18.dp
                    )
                }
            }
        }
    }
}
