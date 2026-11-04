package com.nexus.personalintelligence.core.remote

import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NexusSocketServer(
    private val port: Int = 8765,
    private val tvDeviceName: String = "NEXUS TV",
    private val commandDispatcher: suspend (RemoteCommand) -> CommandResult = {
        CommandResult(commandId = it.commandId, success = true, message = "Executed ${it.type}")
    }
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val secureRandom = SecureRandom()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    val pairedDevices = ConcurrentHashMap<String, PairedDevice>()

    // Pending pairing requests awaiting security review gate approval
    data class PendingPairing(
        val clientDeviceId: String,
        val clientDeviceName: String,
        val clientIp: String,
        val pin: String,
        val createdAt: Long = System.currentTimeMillis(),
        val approveDeferred: CompletableDeferred<Boolean>
    )

    private val pendingPairings = ConcurrentHashMap<String, PendingPairing>()

    var securityGateCallback: ((deviceName: String, clientIp: String, pin: String, onApprove: () -> Unit, onReject: () -> Unit) -> Unit)? = null

    val actualPort: Int
        get() = serverSocket?.localPort ?: port

    fun start() {
        if (isRunning) return
        isRunning = true
        serverSocket = ServerSocket(port)
        scope.launch {
            while (isRunning && serverSocket?.isClosed == false) {
                try {
                    val client = serverSocket?.accept() ?: break
                    launch { handleClient(client) }
                } catch (e: Exception) {
                    if (!isRunning) break
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        scope.cancel()
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
        var currentSessionToken: String? = null
        var currentDeviceId: String? = null

        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            while (isRunning && !socket.isClosed) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) continue

                val packet = try {
                    json.decodeFromString<NetworkPacket>(line)
                } catch (e: Exception) {
                    continue
                }

                when (packet.type) {
                    MessageType.PING -> {
                        val resp = NetworkPacket(type = MessageType.PONG, id = packet.id)
                        writer.println(json.encodeToString(resp))
                    }
                    MessageType.PAIR_REQUEST -> {
                        val req = json.decodeFromString<PairingRequest>(packet.payload)
                        // Generate cryptographically secure 6-digit PIN
                        val pin = String.format("%06d", secureRandom.nextInt(1000000))
                        val approveDeferred = CompletableDeferred<Boolean>()

                        val pending = PendingPairing(
                            clientDeviceId = req.clientDeviceId,
                            clientDeviceName = req.clientDeviceName,
                            clientIp = clientIp,
                            pin = pin,
                            approveDeferred = approveDeferred
                        )
                        pendingPairings[req.clientDeviceId] = pending

                        // Notify security gate callback
                        securityGateCallback?.invoke(
                            req.clientDeviceName,
                            clientIp,
                            pin,
                            { approveDeferred.complete(true) },
                            { approveDeferred.complete(false) }
                        )

                        // Wait for TV user approval (timeout after 60s)
                        val approved = try {
                            withTimeout(60000) { approveDeferred.await() }
                        } catch (e: TimeoutCancellationException) {
                            false
                        }

                        if (approved) {
                            val pairResp = PairingResponse(
                                status = PairingStatus.PENDING_APPROVAL,
                                pin = pin,
                                tvDeviceName = tvDeviceName,
                                message = "Enter the 6-digit PIN on the TV screen"
                            )
                            val resp = NetworkPacket(type = MessageType.PAIR_RESPONSE, id = packet.id, payload = json.encodeToString(pairResp))
                            writer.println(json.encodeToString(resp))
                        } else {
                            pendingPairings.remove(req.clientDeviceId)
                            val pairResp = PairingResponse(
                                status = PairingStatus.REJECTED,
                                tvDeviceName = tvDeviceName,
                                message = "Pairing was rejected or timed out on TV"
                            )
                            val resp = NetworkPacket(type = MessageType.PAIR_RESPONSE, id = packet.id, payload = json.encodeToString(pairResp))
                            writer.println(json.encodeToString(resp))
                        }
                    }
                    MessageType.PAIR_VERIFY -> {
                        val verify = json.decodeFromString<PairingVerification>(packet.payload)
                        val pending = pendingPairings[verify.clientDeviceId]
                        if (pending != null && pending.pin == verify.pin) {
                            val token = UUID.randomUUID().toString()
                            val device = PairedDevice(
                                deviceId = verify.clientDeviceId,
                                deviceName = pending.clientDeviceName,
                                sessionToken = token,
                                pairedAt = System.currentTimeMillis(),
                                lastSeenAt = System.currentTimeMillis()
                            )
                            pairedDevices[verify.clientDeviceId] = device
                            pendingPairings.remove(verify.clientDeviceId)
                            currentSessionToken = token
                            currentDeviceId = verify.clientDeviceId

                            val pairResp = PairingResponse(
                                status = PairingStatus.APPROVED,
                                sessionToken = token,
                                tvDeviceName = tvDeviceName,
                                message = "Pairing successful and authenticated"
                            )
                            val resp = NetworkPacket(type = MessageType.PAIR_RESPONSE, id = packet.id, payload = json.encodeToString(pairResp))
                            writer.println(json.encodeToString(resp))
                        } else {
                            val pairResp = PairingResponse(
                                status = PairingStatus.INVALID_PIN,
                                tvDeviceName = tvDeviceName,
                                message = "Invalid pairing PIN"
                            )
                            val resp = NetworkPacket(type = MessageType.PAIR_RESPONSE, id = packet.id, payload = json.encodeToString(pairResp))
                            writer.println(json.encodeToString(resp))
                        }
                    }
                    MessageType.HANDSHAKE -> {
                        val hs = json.decodeFromString<HandshakeRequest>(packet.payload)
                        val device = pairedDevices[hs.clientDeviceId]
                        if (device == null || device.isRevoked) {
                            val resp = NetworkPacket(
                                type = MessageType.HANDSHAKE_RESPONSE,
                                id = packet.id,
                                payload = json.encodeToString(HandshakeResponse(status = HandshakeStatus.UNAUTHORIZED, tvDeviceName = tvDeviceName, message = "Device not paired or revoked"))
                            )
                            writer.println(json.encodeToString(resp))
                        } else if (device.sessionToken != hs.sessionToken) {
                            val resp = NetworkPacket(
                                type = MessageType.HANDSHAKE_RESPONSE,
                                id = packet.id,
                                payload = json.encodeToString(HandshakeResponse(status = HandshakeStatus.UNAUTHORIZED, tvDeviceName = tvDeviceName, message = "Invalid session token"))
                            )
                            writer.println(json.encodeToString(resp))
                        } else if (System.currentTimeMillis() > device.expiresAt) {
                            val resp = NetworkPacket(
                                type = MessageType.HANDSHAKE_RESPONSE,
                                id = packet.id,
                                payload = json.encodeToString(HandshakeResponse(status = HandshakeStatus.EXPIRED, tvDeviceName = tvDeviceName, message = "Session has expired"))
                            )
                            writer.println(json.encodeToString(resp))
                        } else {
                            device.lastSeenAt = System.currentTimeMillis()
                            currentSessionToken = device.sessionToken
                            currentDeviceId = device.deviceId
                            val resp = NetworkPacket(
                                type = MessageType.HANDSHAKE_RESPONSE,
                                id = packet.id,
                                payload = json.encodeToString(HandshakeResponse(status = HandshakeStatus.SUCCESS, tvDeviceName = tvDeviceName, message = "Authenticated"))
                            )
                            writer.println(json.encodeToString(resp))
                        }
                    }
                    MessageType.COMMAND -> {
                        val env = json.decodeFromString<CommandEnvelope>(packet.payload)
                        val device = currentDeviceId?.let { pairedDevices[it] }
                        if (device == null || device.isRevoked || device.sessionToken != env.sessionToken || System.currentTimeMillis() > device.expiresAt) {
                            val errResult = CommandResult(
                                commandId = env.command.commandId,
                                success = false,
                                message = "Command rejected: unauthorized, expired or revoked session"
                            )
                            val resp = NetworkPacket(type = MessageType.COMMAND_RESULT, id = packet.id, payload = json.encodeToString(errResult))
                            writer.println(json.encodeToString(resp))
                        } else {
                            device.lastSeenAt = System.currentTimeMillis()
                            val result = commandDispatcher(env.command)
                            val resp = NetworkPacket(type = MessageType.COMMAND_RESULT, id = packet.id, payload = json.encodeToString(result))
                            writer.println(json.encodeToString(resp))
                        }
                    }
                    MessageType.DISCONNECT -> {
                        break
                    }
                    else -> {}
                }
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    fun revokeDevice(deviceId: String): Boolean {
        val dev = pairedDevices[deviceId] ?: return false
        dev.isRevoked = true
        return true
    }

    fun removeDevice(deviceId: String): Boolean {
        return pairedDevices.remove(deviceId) != null
    }

    fun getPendingPin(deviceId: String): String? = pendingPairings[deviceId]?.pin

    fun getPairedDevice(deviceId: String): PairedDevice? = pairedDevices[deviceId]

    fun getPairedDevices(): List<PairedDevice> = pairedDevices.values.toList()

    fun isDevicePaired(deviceId: String): Boolean =
        pairedDevices[deviceId]?.let { !it.isRevoked && System.currentTimeMillis() <= it.expiresAt } ?: false

    fun approvePairingManually(deviceId: String) {
        pendingPairings[deviceId]?.approveDeferred?.complete(true)
    }

    fun rejectPairingManually(deviceId: String) {
        pendingPairings[deviceId]?.approveDeferred?.complete(false)
    }
}
