package com.nexus.personalintelligence.mobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.personalintelligence.core.remote.CommandResult
import com.nexus.personalintelligence.core.remote.ConnectionState
import com.nexus.personalintelligence.core.remote.PairingResponse
import com.nexus.personalintelligence.core.remote.PairingStatus
import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.mobile.ai.ProviderDraft
import com.nexus.personalintelligence.mobile.data.AssistantLanguage
import com.nexus.personalintelligence.mobile.data.MobileSettings
import com.nexus.personalintelligence.mobile.data.MobileSettingsRepository
import com.nexus.personalintelligence.mobile.remote.MobileRemoteManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MobileViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MobileSettingsRepository(app)
    val remoteManager = MobileRemoteManager(repo)

    val settings: StateFlow<MobileSettings?> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val connectionState: StateFlow<ConnectionState> = remoteManager.connectionState
    val pairingStatus: StateFlow<PairingStatus?> = remoteManager.pairingStatus
    val remoteStatusMessage: StateFlow<String?> = remoteManager.statusMessage

    fun startPairing(host: String, port: Int = 8765, onComplete: ((PairingResponse) -> Unit)? = null) {
        viewModelScope.launch {
            val resp = remoteManager.startPairing(host, port)
            onComplete?.invoke(resp)
        }
    }

    fun verifyPin(pin: String, onComplete: ((PairingResponse) -> Unit)? = null) {
        viewModelScope.launch {
            val resp = remoteManager.verifyPin(pin)
            onComplete?.invoke(resp)
        }
    }

    fun reconnectTv(onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val current = settings.value?.pairedTv ?: return@launch
            val ok = remoteManager.connectWithSaved(current)
            onResult?.invoke(ok)
        }
    }

    fun disconnectTv() {
        remoteManager.disconnect()
    }

    fun unpairTv() {
        viewModelScope.launch {
            remoteManager.unpair()
        }
    }

    fun sendRemoteCommand(command: RemoteCommand, onResult: ((CommandResult) -> Unit)? = null) {
        viewModelScope.launch {
            val res = remoteManager.sendCommand(command)
            onResult?.invoke(res)
        }
    }

    fun clearRemoteStatusMessage() {
        remoteManager.clearStatusMessage()
    }

    fun setDisplayName(name: String) {
        viewModelScope.launch { repo.setDisplayName(name.trim().take(40)) }
    }

    fun setLanguage(language: AssistantLanguage) {
        viewModelScope.launch { repo.setLanguage(language) }
    }

    /** [onResult] receives null on success or an error message. */
    fun saveProvider(draft: ProviderDraft, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repo.saveProvider(draft)) }
    }

    fun removeApiKey() {
        viewModelScope.launch { repo.removeApiKey() }
    }
}
