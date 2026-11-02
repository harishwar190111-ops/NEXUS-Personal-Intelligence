package com.nexus.personalintelligence.tv.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.personalintelligence.core.common.ui.GlassPanel
import com.nexus.personalintelligence.core.common.ui.NexusColors
import com.nexus.personalintelligence.tv.data.DeviceInfo
import com.nexus.personalintelligence.tv.data.TvPrefs
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    prefs: TvPrefs,
    appCount: Int,
    launcherMode: Boolean,
    browsers: List<String>,
    onUse24Hour: (Boolean) -> Unit,
    onClearRecents: () -> Unit,
    onClearFavourites: () -> Unit,
    onLauncherMode: (Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    val context = LocalContext.current
    val deviceInfo = remember { DeviceInfo.collect(context) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "unknown"
    }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstFocus.requestFocus() }
    }

    ScreenFrame("Settings") {
        SectionTitle("Clock")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvButton("24-hour", { onUse24Hour(true) }, selected = prefs.use24Hour, modifier = Modifier.focusRequester(firstFocus))
            TvButton("12-hour", { onUse24Hour(false) }, selected = !prefs.use24Hour)
        }

        SectionTitle("Your data (stored on this TV only)")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvButton("Clear recently opened", onClearRecents)
            TvButton("Clear favourites", onClearFavourites)
        }

        SectionTitle("Launcher mode (experimental)")
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (launcherMode) "ON: NEXUS TV is registered as a Home app." else "OFF: your original launcher is untouched.",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Turning this on only lets Android list NEXUS as a possible Home app. Whether this Motorola firmware lets you pick it as the default is not verified. " +
                        "To go back: turn this off here, or open Android Settings > Apps > NEXUS TV and clear defaults or uninstall.",
                    fontSize = 13.sp,
                    color = NexusColors.TextSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvButton(if (launcherMode) "Turn off" else "Turn on", { onLauncherMode(!launcherMode) })
                    TvButton("Open Android settings", onOpenSystemSettings)
                }
            }
        }

        SectionTitle("Websites")
        Text(
            if (browsers.isEmpty()) "No browser app found. Web apps cannot open until one is installed." else "Browsers that can open links: ${browsers.joinToString(", ")}",
            fontSize = 14.sp,
            color = NexusColors.TextSecondary,
        )

        SectionTitle("Device information (read from this TV)")
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                deviceInfo.forEach { (label, value) ->
                    Row {
                        Text(label, Modifier.width(210.dp), fontSize = 14.sp, color = NexusColors.TextMuted)
                        Text(value, fontSize = 14.sp, color = NexusColors.TextPrimary)
                    }
                }
            }
        }

        SectionTitle("About")
        Text(
            "NEXUS TV $versionName on Android ${Build.VERSION.RELEASE}. $appCount launchable apps found. " +
                "Permissions requested: none. Phone pairing is not available in this build.",
            fontSize = 14.sp,
            color = NexusColors.TextSecondary,
        )
    }
}
