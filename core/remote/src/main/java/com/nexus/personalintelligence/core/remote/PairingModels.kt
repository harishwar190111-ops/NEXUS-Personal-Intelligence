package com.nexus.personalintelligence.core.remote

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class PairingStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    EXPIRED,
    INVALID_PIN
}

@Serializable
data class PairingRequest(
    val clientDeviceId: String,
    val clientDeviceName: String,
    val clientIp: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PairingResponse(
    val status: PairingStatus,
    val pin: String = "",
    val sessionToken: String? = null,
    val tvDeviceName: String = "NEXUS TV",
    val message: String = "",
    val expiresAt: Long = 0L
)

@Serializable
data class PairingVerification(
    val clientDeviceId: String,
    val pin: String
)

@Serializable
enum class HandshakeStatus {
    SUCCESS,
    UNAUTHORIZED,
    EXPIRED,
    REVOKED
}

@Serializable
data class HandshakeRequest(
    val clientDeviceId: String,
    val sessionToken: String,
    val clientDeviceName: String = ""
)

@Serializable
data class HandshakeResponse(
    val status: HandshakeStatus,
    val tvDeviceName: String = "NEXUS TV",
    val serverVersion: String = "2.0.0",
    val message: String = ""
)

@Serializable
data class PairedDevice(
    val deviceId: String,
    val deviceName: String,
    val sessionToken: String,
    val pairedAt: Long = System.currentTimeMillis(),
    var lastSeenAt: Long = System.currentTimeMillis(),
    var isRevoked: Boolean = false,
    val expiresAt: Long = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
)

@Serializable
data class CommandEnvelope(
    val commandId: String = UUID.randomUUID().toString(),
    val sessionToken: String,
    val command: RemoteCommand
)

@Serializable
data class CommandResult(
    val commandId: String,
    val success: Boolean,
    val message: String = "",
    val resultData: String? = null
)

@Serializable
enum class ConnectionState {
    NOT_PAIRED,
    CONNECTING,
    AWAITING_TV_APPROVAL,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    AUTH_FAILED
}

@Serializable
enum class MessageType {
    PAIR_REQUEST,
    PAIR_RESPONSE,
    PAIR_VERIFY,
    HANDSHAKE,
    HANDSHAKE_RESPONSE,
    COMMAND,
    COMMAND_RESULT,
    PING,
    PONG,
    DISCONNECT,
    TV_MANAGEMENT_REQUEST,
    TV_MANAGEMENT_RESPONSE
}

@Serializable
data class NetworkPacket(
    val type: MessageType,
    val id: String = UUID.randomUUID().toString(),
    val sessionToken: String? = null,
    val payload: String = ""
)
