package com.nexus.personalintelligence.core.remote

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NexusSocketClient(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _connectionState = MutableStateFlow(ConnectionState.NOT_PAIRED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private var listenJob: Job? = null
    private var heartbeatJob: Job? = null

    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<NetworkPacket>>()

    var currentHost: String? = null
        private set
    var currentPort: Int = 8765
        private set
    var currentDeviceId: String? = null
        private set
    var currentSessionToken: String? = null
        private set

    fun isConnected(): Boolean {
        return socket?.isConnected == true && socket?.isClosed == false && _connectionState.value == ConnectionState.CONNECTED
    }

    suspend fun connect(host: String, port: Int = 8765, timeoutMs: Int = 5000): Boolean = withContext(Dispatchers.IO) {
        disconnectInternal(notifyState = false)
        currentHost = host
        currentPort = port
        _connectionState.value = ConnectionState.CONNECTING

        try {
            val sock = Socket()
            sock.connect(InetSocketAddress(host, port), timeoutMs)
            socket = sock
            reader = BufferedReader(InputStreamReader(sock.getInputStream()))
            writer = PrintWriter(sock.getOutputStream(), true)

            startListening()
            startHeartbeat()
            true
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            false
        }
    }

    private fun startListening() {
        listenJob = scope.launch(Dispatchers.IO) {
            try {
                val r = reader ?: return@launch
                while (isActive && socket?.isClosed == false) {
                    val line = r.readLine() ?: break
                    if (line.isBlank()) continue
                    val packet = try {
                        json.decodeFromString<NetworkPacket>(line)
                    } catch (_: Exception) {
                        continue
                    }
                    val deferred = pendingRequests.remove(packet.id)
                    deferred?.complete(packet)
                }
            } catch (_: Exception) {
            } finally {
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && socket?.isClosed == false) {
                delay(5000)
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    val success = ping()
                    if (!success) {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        break
                    }
                }
            }
        }
    }

    private suspend fun sendPacketAndAwait(
        type: MessageType,
        payload: String = "",
        sessionToken: String? = null,
        timeoutMs: Long = 65000
    ): NetworkPacket? = withContext(Dispatchers.IO) {
        val w = writer ?: return@withContext null
        val id = UUID.randomUUID().toString()
        val packet = NetworkPacket(
            type = type,
            id = id,
            sessionToken = sessionToken,
            payload = payload
        )
        val deferred = CompletableDeferred<NetworkPacket>()
        pendingRequests[id] = deferred

        try {
            w.println(json.encodeToString(packet))
            withTimeout(timeoutMs) { deferred.await() }
        } catch (e: Exception) {
            pendingRequests.remove(id)
            null
        }
    }

    suspend fun ping(): Boolean {
        val resp = sendPacketAndAwait(type = MessageType.PING, timeoutMs = 3000)
        return resp?.type == MessageType.PONG
    }

    suspend fun requestPairing(deviceId: String, deviceName: String): PairingResponse {
        currentDeviceId = deviceId
        val req = PairingRequest(clientDeviceId = deviceId, clientDeviceName = deviceName)
        val packet = sendPacketAndAwait(
            type = MessageType.PAIR_REQUEST,
            payload = json.encodeToString(req),
            timeoutMs = 65000
        ) ?: return PairingResponse(status = PairingStatus.REJECTED, message = "Pairing request timed out or network error")

        val resp = json.decodeFromString<PairingResponse>(packet.payload)
        if (resp.status == PairingStatus.PENDING_APPROVAL) {
            _connectionState.value = ConnectionState.AWAITING_TV_APPROVAL
        } else {
            _connectionState.value = ConnectionState.NOT_PAIRED
        }
        return resp
    }

    suspend fun verifyPairing(deviceId: String, pin: String): PairingResponse {
        currentDeviceId = deviceId
        val verify = PairingVerification(clientDeviceId = deviceId, pin = pin)
        val packet = sendPacketAndAwait(
            type = MessageType.PAIR_VERIFY,
            payload = json.encodeToString(verify),
            timeoutMs = 5000
        ) ?: return PairingResponse(status = PairingStatus.INVALID_PIN, message = "Verification response timed out")

        val resp = json.decodeFromString<PairingResponse>(packet.payload)
        if (resp.status == PairingStatus.APPROVED && resp.sessionToken != null) {
            currentSessionToken = resp.sessionToken
            _connectionState.value = ConnectionState.CONNECTED
        } else {
            _connectionState.value = ConnectionState.NOT_PAIRED
        }
        return resp
    }

    suspend fun authenticate(
        deviceId: String,
        sessionToken: String,
        deviceName: String = ""
    ): HandshakeResponse {
        currentDeviceId = deviceId
        currentSessionToken = sessionToken
        val hs = HandshakeRequest(clientDeviceId = deviceId, sessionToken = sessionToken, clientDeviceName = deviceName)
        val packet = sendPacketAndAwait(
            type = MessageType.HANDSHAKE,
            payload = json.encodeToString(hs),
            timeoutMs = 5000
        ) ?: return HandshakeResponse(status = HandshakeStatus.UNAUTHORIZED, message = "Handshake timed out")

        val resp = json.decodeFromString<HandshakeResponse>(packet.payload)
        if (resp.status == HandshakeStatus.SUCCESS) {
            _connectionState.value = ConnectionState.CONNECTED
        } else {
            _connectionState.value = ConnectionState.AUTH_FAILED
        }
        return resp
    }

    suspend fun sendCommand(command: RemoteCommand, sessionToken: String? = currentSessionToken): CommandResult {
        val token = sessionToken ?: currentSessionToken ?: return CommandResult(
            commandId = command.commandId,
            success = false,
            message = "Not authenticated"
        )
        val env = CommandEnvelope(
            commandId = command.commandId,
            sessionToken = token,
            command = command
        )
        val packet = sendPacketAndAwait(
            type = MessageType.COMMAND,
            payload = json.encodeToString(env),
            sessionToken = token,
            timeoutMs = 5000
        ) ?: return CommandResult(
            commandId = command.commandId,
            success = false,
            message = "Command timed out"
        )

        return try {
            json.decodeFromString<CommandResult>(packet.payload)
        } catch (e: Exception) {
            CommandResult(commandId = command.commandId, success = false, message = "Failed to parse command result: ${e.message}")
        }
    }

    suspend fun reconnect(): Boolean {
        val host = currentHost ?: return false
        val port = currentPort
        val devId = currentDeviceId ?: return false
        val token = currentSessionToken ?: return false

        _connectionState.value = ConnectionState.RECONNECTING
        val connected = connect(host, port)
        if (!connected) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return false
        }
        val auth = authenticate(devId, token)
        return auth.status == HandshakeStatus.SUCCESS
    }

    private fun disconnectInternal(notifyState: Boolean) {
        try {
            writer?.println(json.encodeToString(NetworkPacket(type = MessageType.DISCONNECT)))
        } catch (_: Exception) {}

        listenJob?.cancel()
        heartbeatJob?.cancel()
        pendingRequests.values.forEach { it.cancel() }
        pendingRequests.clear()

        try { socket?.close() } catch (_: Exception) {}
        socket = null
        reader = null
        writer = null
        if (notifyState) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun disconnect() {
        disconnectInternal(notifyState = true)
    }
}
