package com.nexus.personalintelligence.tv

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.personalintelligence.core.common.ui.BackgroundSpec
import com.nexus.personalintelligence.core.common.ui.BackgroundType
import com.nexus.personalintelligence.core.common.ui.CustomTheme
import com.nexus.personalintelligence.core.remote.PairedDevice
import com.nexus.personalintelligence.tv.network.SecurityGateRequest
import com.nexus.personalintelligence.tv.network.TvCommandDispatcher
import com.nexus.personalintelligence.tv.network.TvNetworkManager
import com.nexus.personalintelligence.tv.data.AppEntry
import com.nexus.personalintelligence.tv.data.AppIcon
import com.nexus.personalintelligence.tv.data.AppRepository
import com.nexus.personalintelligence.tv.data.LaunchResult
import com.nexus.personalintelligence.tv.data.TvPrefs
import com.nexus.personalintelligence.tv.data.TvPrefsRepository
import com.nexus.personalintelligence.tv.data.WebLogic
import com.nexus.personalintelligence.tv.data.WebShortcut
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TvViewModel(app: Application) : AndroidViewModel(app) {
    private val appRepo = AppRepository(app)
    private val prefsRepo = TvPrefsRepository(app)
    val networkManager = TvNetworkManager()
    private val commandDispatcher = TvCommandDispatcher(app, appRepo)

    val securityGateRequest: StateFlow<SecurityGateRequest?> = networkManager.securityGateRequest
    val pairedDevices: StateFlow<List<PairedDevice>> = networkManager.pairedDevices

    private val _apps = MutableStateFlow<List<AppEntry>?>(null)
    val apps: StateFlow<List<AppEntry>?> = _apps

    val prefs: StateFlow<TvPrefs?> = prefsRepo.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _launcherMode = MutableStateFlow(appRepo.launcherModeEnabled())
    val launcherMode: StateFlow<Boolean> = _launcherMode

    private var refreshJob: Job? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(app, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        networkManager.commandHandler = { cmd -> commandDispatcher.dispatch(cmd) }
        networkManager.start()
        refresh()
    }

    /** Re-scans installed apps. Called on start, on resume and when packages change. */
    fun refresh() {
        _launcherMode.value = appRepo.launcherModeEnabled()
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _apps.value = try {
                appRepo.discover()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun loadIcon(entry: AppEntry): AppIcon? = appRepo.loadIcon(entry)

    fun launchApp(entry: AppEntry) {
        when (val result = appRepo.launch(entry.packageName)) {
            is LaunchResult.Started -> viewModelScope.launch { prefsRepo.pushRecent(entry.packageName) }
            is LaunchResult.Failed -> _message.value = "Couldn't open ${entry.label}: ${result.reason}."
        }
    }

    fun openSystemSettings() {
        val result = appRepo.openSystemSettings()
        if (result is LaunchResult.Failed) _message.value = result.reason.replaceFirstChar { it.uppercase() } + "."
    }

    fun showMessage(text: String) {
        _message.value = text
    }

    fun clearMessage() {
        _message.value = null
    }

    // ---- favourites / recents ----
    fun toggleFavourite(pkg: String) = viewModelScope.launch { prefsRepo.toggleFavourite(pkg) }
    fun clearRecents() = viewModelScope.launch { prefsRepo.clearRecents() }
    fun clearFavourites() = viewModelScope.launch { prefsRepo.clearFavourites() }
    fun setUse24Hour(value: Boolean) = viewModelScope.launch { prefsRepo.setUse24Hour(value) }

    // ---- theme + background ----
    fun setPreset(id: String) = viewModelScope.launch { prefsRepo.setPreset(id) }
    fun setCustom(custom: CustomTheme) = viewModelScope.launch { prefsRepo.setCustom(custom) }
    fun setBackground(spec: BackgroundSpec) = viewModelScope.launch { prefsRepo.setBackground(spec) }
    fun resetBackground() = viewModelScope.launch { prefsRepo.resetBackground() }

    fun importBackgroundImage(uri: Uri) = viewModelScope.launch {
        val file = appRepo.importImage(uri)
        if (file == null) {
            _message.value = "That image couldn't be read."
            return@launch
        }
        val old = prefs.value?.imagePath
        prefsRepo.setImagePath(file.absolutePath)
        val current = prefs.value?.theme?.background ?: BackgroundSpec()
        prefsRepo.setBackground(current.copy(type = BackgroundType.Image))
        if (old != null && old != file.absolutePath) runCatching { File(old).delete() }
    }

    // ---- web shortcuts ----
    fun addWebsite(name: String, url: String) = viewModelScope.launch {
        prefsRepo.editWebApps { list ->
            list.filterNot { it.url == url } + WebShortcut(WebLogic.cleanName(name).ifBlank { WebLogic.defaultName(url) }, url)
        }
    }

    fun renameWebsite(url: String, name: String) = viewModelScope.launch {
        prefsRepo.editWebApps { list ->
            list.map { if (it.url == url) it.copy(name = WebLogic.cleanName(name).ifBlank { it.name }) else it }
        }
    }

    fun removeWebsite(url: String) = viewModelScope.launch {
        prefsRepo.editWebApps { list -> list.filterNot { it.url == url } }
    }

    fun openWebsite(site: WebShortcut) {
        val result = appRepo.openUrl(site.url)
        if (result is LaunchResult.Failed) _message.value = "Couldn't open ${site.name}: ${result.reason}."
    }

    fun browserNames(): List<String> = appRepo.browserNames()

    // ---- launcher mode ----
    fun setLauncherMode(enabled: Boolean) {
        if (appRepo.setLauncherMode(enabled)) {
            _launcherMode.value = appRepo.launcherModeEnabled()
        } else {
            _message.value = "Android refused to change launcher mode."
        }
    }

    fun revokePairedDevice(deviceId: String): Boolean = networkManager.revokeDevice(deviceId)

    fun removePairedDevice(deviceId: String): Boolean = networkManager.removeDevice(deviceId)

    override fun onCleared() {
        networkManager.stop()
        try {
            getApplication<Application>().unregisterReceiver(packageReceiver)
        } catch (e: IllegalArgumentException) {
            // already unregistered
        }
        super.onCleared()
    }
}
