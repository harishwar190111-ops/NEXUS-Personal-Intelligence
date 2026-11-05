package com.nexus.personalintelligence.mobile.remote

import android.content.Context
import android.os.Build
import com.nexus.personalintelligence.core.remote.CommandResult
import com.nexus.personalintelligence.core.remote.ConnectionState
import com.nexus.personalintelligence.core.remote.NexusSocketClient
import com.nexus.personalintelligence.core.remote.PairingResponse
import com.nexus.personalintelligence.core.remote.PairingStatus
import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import com.nexus.personalintelligence.core.remote.diagnostics.CommandHistoryEntry
import com.nexus.personalintelligence.core.remote.diagnostics.CommandHistoryTracker
import com.nexus.personalintelligence.core.remote.events.EventCategory
import com.nexus.personalintelligence.core.remote.events.EventSeverity
import com.nexus.personalintelligence.core.remote.events.NexusEvent
import com.nexus.personalintelligence.core.remote.events.NexusEventStore
import com.nexus.personalintelligence.mobile.data.PairedTvInfo
import com.nexus.personalintelligence.mobile.data.PairedTvStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MobileRemoteManager(
    private val storage: PairedTvStorage,
    private val client: NexusSocketClient = NexusSocketClient(),
    val historyTracker: CommandHistoryTracker = CommandHistoryTracker(),
    val eventStore: NexusEventStore = NexusEventStore(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    val connectionState: StateFlow<ConnectionState> = client.connectionState

    private val _pairingStatus = MutableStateFlow<PairingStatus?>(null)
    val pairingStatus: StateFlow<PairingStatus?> = _pairingStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var currentHost: String = ""
    private var currentPort: Int = 8765
    private var myDeviceId: String = UUID.randomUUID().toString()

    suspend fun startPairing(host: String, port: Int = 8765, deviceName: String = Build.MODEL ?: "Android Phone"): PairingResponse {
        currentHost = host
        currentPort = port
        _statusMessage.value = "Connecting to TV at $host:$port..."

        val connected = client.connect(host, port)
        if (!connected) {
            _pairingStatus.value = PairingStatus.REJECTED
            _statusMessage.value = "Could not reach TV at $host:$port. Ensure both devices are on the same Wi-Fi network."
            eventStore.addEvent(
                NexusEvent(
                    title = "Pairing Failed",
                    message = "Could not reach TV at $host:$port",
                    severity = EventSeverity.WARNING,
                    category = EventCategory.PAIRING
                )
            )
            return PairingResponse(status = PairingStatus.REJECTED, message = "Connection failed")
        }

        _statusMessage.value = "Awaiting TV approval..."
        val pairResp = client.requestPairing(myDeviceId, deviceName)
        _pairingStatus.value = pairResp.status

        if (pairResp.status == PairingStatus.PENDING_APPROVAL) {
            _statusMessage.value = "Pairing approved on TV. Please enter the 6-digit PIN shown on the TV screen."
            eventStore.addEvent(
                NexusEvent(
                    title = "Pairing Pending",
                    message = "Awaiting 6-digit PIN verification for $deviceName",
                    severity = EventSeverity.INFO,
                    category = EventCategory.PAIRING
                )
            )
        } else {
            _statusMessage.value = pairResp.message.ifBlank { "Pairing rejected by TV" }
            eventStore.addEvent(
                NexusEvent(
                    title = "Pairing Rejected",
                    message = pairResp.message.ifBlank { "Pairing rejected by TV" },
                    severity = EventSeverity.WARNING,
                    category = EventCategory.PAIRING
                )
            )
        }
        return pairResp
    }

    suspend fun verifyPin(pin: String): PairingResponse {
        _statusMessage.value = "Verifying PIN..."
        val verifyResp = client.verifyPairing(myDeviceId, pin)
        _pairingStatus.value = verifyResp.status

        if (verifyResp.status == PairingStatus.APPROVED && verifyResp.sessionToken != null) {
            _statusMessage.value = "Pairing successful! Connected to TV."
            val info = PairedTvInfo(
                host = currentHost,
                port = currentPort,
                tvDeviceName = verifyResp.tvDeviceName,
                sessionToken = verifyResp.sessionToken!!,
                deviceId = myDeviceId,
                pairedAt = System.currentTimeMillis()
            )
            val saveResult = storage.savePairedTv(info)
            if (saveResult.isFailure) {
                _statusMessage.value = "Failed to securely store pairing token"
                eventStore.addEvent(
                    NexusEvent(
                        title = "Storage Error",
                        message = "Could not encrypt and save session token",
                        severity = EventSeverity.ALERT,
                        category = EventCategory.PAIRING
                    )
                )
                return verifyResp.copy(status = PairingStatus.REJECTED, message = "Failed to securely store token")
            }
            eventStore.addEvent(
                NexusEvent(
                    title = "Pairing Complete",
                    message = "Paired with ${verifyResp.tvDeviceName}",
                    severity = EventSeverity.SUCCESS,
                    category = EventCategory.PAIRING
                )
            )
        } else if (verifyResp.status == PairingStatus.INVALID_PIN) {
            _statusMessage.value = verifyResp.message.ifBlank { "Invalid PIN. Please check the TV screen." }
            eventStore.addEvent(
                NexusEvent(
                    title = "Invalid PIN",
                    message = "Incorrect PIN entered",
                    severity = EventSeverity.WARNING,
                    category = EventCategory.PAIRING
                )
            )
        } else if (verifyResp.status == PairingStatus.EXPIRED) {
            _statusMessage.value = "PIN expired. Please initiate pairing again."
            eventStore.addEvent(
                NexusEvent(
                    title = "PIN Expired",
                    message = "PIN expired",
                    severity = EventSeverity.WARNING,
                    category = EventCategory.PAIRING
                )
            )
        } else {
            _statusMessage.value = verifyResp.message.ifBlank { "Pairing failed" }
        }
        return verifyResp
    }

    suspend fun connectWithSaved(info: PairedTvInfo): Boolean {
        if (!info.isPaired) return false
        currentHost = info.host
        currentPort = info.port
        myDeviceId = info.deviceId

        _statusMessage.value = "Reconnecting to ${info.tvDeviceName}..."
        val connected = client.connect(info.host, info.port)
        if (!connected) {
            _statusMessage.value = "TV is unreachable at ${info.host}."
            eventStore.addEvent(
                NexusEvent(
                    title = "Reconnect Failed",
                    message = "TV unreachable at ${info.host}",
                    severity = EventSeverity.WARNING,
                    category = EventCategory.PAIRING
                )
            )
            return false
        }

        val auth = client.authenticate(info.deviceId, info.sessionToken, Build.MODEL ?: "Android Phone")
        if (auth.status == com.nexus.personalintelligence.core.remote.HandshakeStatus.SUCCESS) {
            _statusMessage.value = "Connected to ${info.tvDeviceName}"
            eventStore.addEvent(
                NexusEvent(
                    title = "Connected",
                    message = "Connected to ${info.tvDeviceName}",
                    severity = EventSeverity.SUCCESS,
                    category = EventCategory.PAIRING
                )
            )
            return true
        } else {
            _statusMessage.value = "Authentication failed: ${auth.message}"
            eventStore.addEvent(
                NexusEvent(
                    title = "Auth Failed",
                    message = auth.message,
                    severity = EventSeverity.ALERT,
                    category = EventCategory.PAIRING
                )
            )
            return false
        }
    }

    suspend fun sendCommand(command: RemoteCommand): CommandResult {
        val result = client.sendCommand(command)
        val cmdText = command.text
        val safeMessage = if (result.success) {
            if (command.type == RemoteCommandType.TEXT_INPUT) "Text input delivered" else result.message
        } else {
            val failureText = if (result.message.isNotBlank()) result.message else "Command ${command.type} failed on TV"
            if (!cmdText.isNullOrBlank()) failureText.replace(cmdText, "[REDACTED]") else failureText
        }
        val safeResult = result.copy(message = safeMessage)
        historyTracker.addEntry(
            CommandHistoryEntry(
                commandId = command.commandId,
                type = command.type.name,
                success = safeResult.success,
                message = safeResult.message
            )
        )
        eventStore.addEvent(
            NexusEvent(
                title = "Command: ${command.type.name}",
                message = safeResult.message,
                severity = if (safeResult.success) EventSeverity.INFO else EventSeverity.WARNING,
                category = EventCategory.COMMAND
            )
        )
        return safeResult
    }

    fun disconnect() {
        client.disconnect()
        _statusMessage.value = "Disconnected from TV"
    }

    suspend fun unpair() {
        disconnect()
        _pairingStatus.value = null
        _statusMessage.value = null
        storage.clearPairedTv()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
