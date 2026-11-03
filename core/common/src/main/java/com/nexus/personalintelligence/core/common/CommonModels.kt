package com.nexus.personalintelligence.core.common

import kotlinx.serialization.Serializable

@Serializable
enum class TransportType {
    LOCAL_WIFI,
    WIFI_DIRECT,
    HOTSPOT,
    BLUETOOTH,
    USB_FALLBACK
}

@Serializable
enum class ConnectionState {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    PAIRING_REQUESTED,
    PAIRING_APPROVED,
    PAIRING_REJECTED,
    AUTHENTICATING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

@Serializable
data class DeviceIdentity(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val model: String = "",
    val osVersion: String = "",
    val appVersion: String = "2.0.0"
)

sealed class NexusResult<out T> {
    data class Success<out T>(val data: T) : NexusResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : NexusResult<Nothing>()
}

interface NexusDispatcherProvider {
    val main: kotlinx.coroutines.CoroutineDispatcher
    val io: kotlinx.coroutines.CoroutineDispatcher
    val default: kotlinx.coroutines.CoroutineDispatcher
}

class DefaultDispatcherProvider : NexusDispatcherProvider {
    override val main = kotlinx.coroutines.Dispatchers.Main
    override val io = kotlinx.coroutines.Dispatchers.IO
    override val default = kotlinx.coroutines.Dispatchers.Default
}
