package com.nexus.personalintelligence.tv.ui

import android.content.ActivityNotFoundException
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.personalintelligence.core.common.ui.CustomTheme
import com.nexus.personalintelligence.core.common.ui.GlassPanel
import com.nexus.personalintelligence.core.common.ui.NexusBackground
import com.nexus.personalintelligence.core.common.ui.NexusTheme
import com.nexus.personalintelligence.core.common.ui.NexusWordmark
import com.nexus.personalintelligence.core.common.ui.ThemePresets
import com.nexus.personalintelligence.tv.TvViewModel
import com.nexus.personalintelligence.tv.data.WebLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun NexusTvApp(vm: TvViewModel, startedAsHome: Boolean) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val launcherMode by vm.launcherMode.collectAsStateWithLifecycle()
    val securityGateRequest by vm.securityGateRequest.collectAsStateWithLifecycle()
    val pairedDevices by vm.pairedDevices.collectAsStateWithLifecycle()
    val isScreensaverActive by vm.isScreensaverActive.collectAsStateWithLifecycle()
    val isQuickSettingsOpen by vm.isQuickSettingsOpen.collectAsStateWithLifecycle()
    val homeTrigger by vm.homeNavigationTrigger.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(TvScreen.Home) }
    var entry by remember { mutableStateOf<EntryRequest?>(null) }

    LaunchedEffect(homeTrigger) {
        if (homeTrigger > 0) {
            screen = TvScreen.Home
            entry = null
        }
    }

    val p = prefs
    val a = apps
    val theme = p?.theme ?: ThemePresets.resolve(null, CustomTheme(), null)

    val image by produceState<ImageBitmap?>(null, p?.imagePath) {
        val path = p?.imagePath
        value = if (path == null) {
            null
        } else {
            withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importBackgroundImage(uri)
    }
    val importImage: () -> Unit = {
        try {
            picker.launch("image/*")
        } catch (e: ActivityNotFoundException) {
            vm.showMessage("This TV has no file picker, so an image can't be chosen here.")
        }
    }

    NexusTheme(accent = theme.accent, style = theme.style) {
        NexusBackground(theme.backdrop, theme.accent, spec = theme.background, image = image) {
            BackHandler(enabled = entry != null) { entry = null }
            BackHandler(enabled = entry == null && screen != TvScreen.Home) { screen = TvScreen.Home }
            // When NEXUS is the Home app, Back on the dashboard must do nothing (a launcher never "exits").
            BackHandler(enabled = entry == null && screen == TvScreen.Home && (startedAsHome || launcherMode)) { }

            if (p == null || a == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NexusWordmark(textSize = 34.sp, markSize = 40.dp)
                }
            } else {
                val request = entry
                if (request != null) {
                    when (request) {
                        EntryRequest.NewUrl -> TextEntryScreen(
                            title = "Add website",
                            hint = "Type the web address, for example example.com",
                            initial = "",
                            extraKeys = listOf(".", "/", ":", "-", "_", ".com"),
                            onSubmit = { raw ->
                                val url = WebLogic.normalizeUrl(raw)
                                if (url == null) vm.showMessage("That doesn't look like a web address.") else entry = EntryRequest.NewName(url)
                            },
                            onCancel = { entry = null },
                        )
                        is EntryRequest.NewName -> TextEntryScreen(
                            title = "Name this website",
                            hint = "A short name shown in NEXUS",
                            initial = WebLogic.defaultName(request.url),
                            extraKeys = emptyList(),
                            onSubmit = { name ->
                                vm.addWebsite(name, request.url)
                                entry = null
                            },
                            onCancel = { entry = null },
                        )
                        is EntryRequest.Rename -> TextEntryScreen(
                            title = "Rename website",
                            hint = request.url,
                            initial = request.current,
                            extraKeys = emptyList(),
                            onSubmit = { name ->
                                vm.renameWebsite(request.url, name)
                                entry = null
                            },
                            onCancel = { entry = null },
                        )
                    }
                } else {
                    when (screen) {
                        TvScreen.Home -> HomeScreen(
                            apps = a,
                            prefs = p,
                            pairedPhoneCount = pairedDevices.size,
                            loadIcon = { vm.loadIcon(it) },
                            onLaunch = { vm.launchApp(it) },
                            onNavigate = { screen = it },
                            onOpenQuickSettings = { vm.openQuickSettings() },
                        )
                        TvScreen.AllApps -> AllAppsScreen(
                            apps = a,
                            favourites = p.favourites,
                            loadIcon = { vm.loadIcon(it) },
                            onLaunch = { vm.launchApp(it) },
                            onToggleFavourite = { vm.toggleFavourite(it.packageName) },
                        )
                        TvScreen.Search -> SearchScreen(
                            apps = a,
                            favourites = p.favourites,
                            loadIcon = { vm.loadIcon(it) },
                            onLaunch = { vm.launchApp(it) },
                        )
                        TvScreen.WebApps -> WebAppsScreen(
                            sites = p.webApps,
                            browsers = remember(screen) { vm.browserNames() },
                            onOpen = { vm.openWebsite(it) },
                            onAdd = { entry = EntryRequest.NewUrl },
                            onRename = { entry = EntryRequest.Rename(it.url, it.name) },
                            onRemove = { vm.removeWebsite(it.url) },
                        )
                        TvScreen.Themes -> ThemeStudioScreen(
                            presetId = p.presetId,
                            custom = p.custom,
                            onPick = { vm.setPreset(it) },
                            onCustom = { vm.setCustom(it) },
                        )
                        TvScreen.Backgrounds -> BackgroundStudioScreen(
                            spec = theme.background,
                            onChange = { vm.setBackground(it) },
                            onRestoreDefault = { vm.resetBackground() },
                            onImportImage = importImage,
                        )
                        TvScreen.Settings -> SettingsScreen(
                            prefs = p,
                            appCount = a.size,
                            launcherMode = launcherMode,
                            browsers = remember(screen) { vm.browserNames() },
                            pairedDevices = pairedDevices,
                            onUse24Hour = { vm.setUse24Hour(it) },
                            onClearRecents = { vm.clearRecents() },
                            onClearFavourites = { vm.clearFavourites() },
                            onLauncherMode = { vm.setLauncherMode(it) },
                            onOpenSystemSettings = { vm.openSystemSettings() },
                            onRevokeDevice = { vm.revokePairedDevice(it) },
                            onRemoveDevice = { vm.removePairedDevice(it) },
                        )
                    }
                }

                message?.let { text ->
                    LaunchedEffect(text) {
                        delay(4000)
                        vm.clearMessage()
                    }
                    GlassPanel(Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)) {
                        Text(text, Modifier.padding(horizontal = 28.dp, vertical = 14.dp), fontSize = 16.sp)
                    }
                }

                securityGateRequest?.let { gateReq ->
                    SecurityReviewGateDialog(
                        request = gateReq,
                        onDismiss = { vm.networkManager.dismissGate() }
                    )
                }

                if (isQuickSettingsOpen) {
                    QuickSettingsDialog(
                        use24Hour = p.use24Hour,
                        launcherMode = launcherMode,
                        pairedPhoneCount = pairedDevices.size,
                        activeProfileName = vm.profileStore.getActiveProfile().name,
                        onUse24Hour = { vm.setUse24Hour(it) },
                        onLauncherMode = { vm.setLauncherMode(it) },
                        onStartScreensaver = { vm.startScreensaver() },
                        onOpenFullSettings = { screen = TvScreen.Settings },
                        onDismiss = { vm.closeQuickSettings() }
                    )
                }

                if (isScreensaverActive) {
                    TvScreensaver(
                        use24Hour = p.use24Hour,
                        onWake = { vm.dismissScreensaver() }
                    )
                }
            }
        }
    }
}
