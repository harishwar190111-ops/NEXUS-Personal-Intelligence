package com.nexus.personalintelligence.tv.network

import android.content.Context
import android.os.Build
import com.nexus.personalintelligence.core.remote.CommandResult
import com.nexus.personalintelligence.core.remote.NexusSocketServer
import com.nexus.personalintelligence.core.remote.PairedDevice
import com.nexus.personalintelligence.core.remote.RemoteCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface

data class SecurityGateRequest(
    val clientDeviceName: String,
    val clientIp: String,
    val pin: String,
    val onApprove: () -> Unit,
    val onReject: () -> Unit
)

class TvNetworkManager(
    private val port: Int = 8765,
    private val tvDeviceName: String = Build.MODEL ?: "NEXUS TV",
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val _securityGateRequest = MutableStateFlow<SecurityGateRequest?>(null)
    val securityGateRequest: StateFlow<SecurityGateRequest?> = _securityGateRequest.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<PairedDevice>>(emptyList())
    val pairedDevices: StateFlow<List<PairedDevice>> = _pairedDevices.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    var commandHandler: (suspend (RemoteCommand) -> CommandResult)? = null

    private val server = NexusSocketServer(
        port = port,
        tvDeviceName = tvDeviceName,
        commandDispatcher = { cmd ->
            commandHandler?.invoke(cmd) ?: CommandResult(
                commandId = cmd.commandId,
                success = true,
                message = "Executed ${cmd.type} on TV"
            )
        }
    )

    init {
        server.securityGateCallback = { deviceName, clientIp, pin, onApprove, onReject ->
            _securityGateRequest.value = SecurityGateRequest(
                clientDeviceName = deviceName,
                clientIp = clientIp,
                pin = pin,
                onApprove = {
                    _securityGateRequest.value = null
                    onApprove()
                    updatePairedDevices()
                },
                onReject = {
                    _securityGateRequest.value = null
                    onReject()
                    updatePairedDevices()
                }
            )
        }
    }

    fun start() {
        if (_isServerRunning.value) return
        server.start()
        _isServerRunning.value = true
        updatePairedDevices()
    }

    fun stop() {
        if (!_isServerRunning.value) return
        server.stop()
        _isServerRunning.value = false
        _securityGateRequest.value = null
    }

    fun revokeDevice(deviceId: String): Boolean {
        val ok = server.revokeDevice(deviceId)
        if (ok) updatePairedDevices()
        return ok
    }

    fun removeDevice(deviceId: String): Boolean {
        val ok = server.removeDevice(deviceId)
        if (ok) updatePairedDevices()
        return ok
    }

    fun updatePairedDevices() {
        _pairedDevices.value = server.getPairedDevices()
    }

    fun dismissGate() {
        _securityGateRequest.value?.onReject?.invoke()
        _securityGateRequest.value = null
    }

    val actualPort: Int
        get() = server.actualPort

    companion object {
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
                for (intf in interfaces) {
                    if (intf.isLoopback || !intf.isUp) continue
                    val addrs = intf.inetAddresses ?: continue
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank()) return host
                        }
                    }
                }
            } catch (_: Exception) {}
            return "127.0.0.1"
        }
    }
}
