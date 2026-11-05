package com.nexus.personalintelligence.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.personalintelligence.core.common.ui.LocalNexusAccent
import com.nexus.personalintelligence.core.common.ui.NexusAccent
import com.nexus.personalintelligence.core.common.ui.NexusBackdrop
import com.nexus.personalintelligence.core.common.ui.NexusBackground
import com.nexus.personalintelligence.core.common.ui.NexusColors
import com.nexus.personalintelligence.core.common.ui.NexusTheme
import com.nexus.personalintelligence.mobile.MobileViewModel

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Remote("Remote", Icons.Filled.PlayArrow),
    Assistant("Assistant", Icons.Filled.Face),
    Devices("Devices", Icons.Filled.Phone),
    Settings("Settings", Icons.Filled.Settings),
    More("More", Icons.Filled.Menu),
}

@Composable
fun NexusMobileApp(vm: MobileViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val connectionState by vm.connectionState.collectAsStateWithLifecycle()
    val pairingStatus by vm.pairingStatus.collectAsStateWithLifecycle()
    val remoteStatusMessage by vm.remoteStatusMessage.collectAsStateWithLifecycle()

    var tabName by rememberSaveable { mutableStateOf(Tab.Home.name) }
    val tab = Tab.entries.firstOrNull { it.name == tabName } ?: Tab.Home
    val s = settings

    NexusTheme {
        NexusBackground(NexusBackdrop.Midnight, NexusAccent.Cyan) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = { NavBar(tab) { tabName = it.name } },
            ) { inner ->
                Box(Modifier.fillMaxSize().padding(inner).consumeWindowInsets(inner)) {
                    BackHandler(enabled = tab != Tab.Home) { tabName = Tab.Home.name }
                    if (s != null) {
                        Crossfade(targetState = tab, animationSpec = tween(180), label = "tabs") { t ->
                            when (t) {
                                Tab.Home -> HomeScreen(s, connectionState) { tabName = it.name }
                                Tab.Remote -> RemoteScreen(
                                    pairedTv = s.pairedTv,
                                    connectionState = connectionState,
                                    onSendCommand = { vm.sendRemoteCommand(it) },
                                    onNavigateToDevices = { tabName = Tab.Devices.name }
                                )
                                Tab.Assistant -> AssistantScreen(s)
                                Tab.Devices -> DevicesScreen(
                                    settings = s,
                                    connectionState = connectionState,
                                    pairingStatus = pairingStatus,
                                    statusMessage = remoteStatusMessage,
                                    onStartPairing = { vm.startPairing(it) },
                                    onVerifyPin = { vm.verifyPin(it) },
                                    onReconnect = { vm.reconnectTv() },
                                    onDisconnect = { vm.disconnectTv() },
                                    onUnpair = { vm.unpairTv() },
                                    onNavigateToRemote = { tabName = Tab.Remote.name }
                                )
                                Tab.Settings -> SettingsScreen(
                                    settings = s,
                                    onName = { vm.setDisplayName(it) },
                                    onLanguage = { vm.setLanguage(it) },
                                    onSaveProvider = { draft, done -> vm.saveProvider(draft, done) },
                                    onRemoveKey = { vm.removeApiKey() },
                                )
                                Tab.More -> MoreScreen { tabName = it.name }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavBar(current: Tab, onSelect: (Tab) -> Unit) {
    val accent = LocalNexusAccent.current.color
    NavigationBar(containerColor = Color(0xE60A1220), tonalElevation = 0.dp) {
        Tab.entries.forEach { t ->
            NavigationBarItem(
                selected = t == current,
                onClick = { onSelect(t) },
                icon = { Icon(t.icon, contentDescription = t.label) },
                label = { Text(t.label, fontSize = 11.sp, maxLines = 1) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    selectedTextColor = accent,
                    indicatorColor = accent.copy(alpha = 0.16f),
                    unselectedIconColor = NexusColors.TextMuted,
                    unselectedTextColor = NexusColors.TextMuted,
                ),
            )
        }
    }
}
