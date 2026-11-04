package com.nexus.personalintelligence.core.remote

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class PairingProtocolTest {

    private var server: NexusSocketServer? = null
    private var client: NexusSocketClient? = null

    @Before
    fun setUp() {
        server = NexusSocketServer(
            port = 0,
            tvDeviceName = "Motorola TV Stick",
            commandDispatcher = { cmd ->
                CommandResult(
                    commandId = cmd.commandId,
                    success = true,
                    message = "Executed ${cmd.type} on TV"
                )
            }
        )
        server?.start()
        client = NexusSocketClient()
    }

    @After
    fun tearDown() {
        client?.disconnect()
        server?.stop()
    }

    @Test
    fun testSuccessfulPairingAndCommandExecution() = runBlocking {
        val s = server ?: throw IllegalStateException("Server not initialized")
        val c = client ?: throw IllegalStateException("Client not initialized")

        val capturedPin = AtomicReference<String>()
        s.securityGateCallback = { deviceName, clientIp, pin, onApprove, _ ->
            assertEquals("Samsung Galaxy F54", deviceName)
            assertEquals(6, pin.length)
            capturedPin.set(pin)
            onApprove()
        }

        val connected = c.connect("127.0.0.1", s.actualPort)
        assertTrue("Client should connect to server", connected)

        val pairResp = c.requestPairing("phone-123", "Samsung Galaxy F54")
        assertEquals(PairingStatus.PENDING_APPROVAL, pairResp.status)
        assertEquals(ConnectionState.AWAITING_TV_APPROVAL, c.connectionState.value)

        val pin = capturedPin.get()
        assertNotNull("Security review gate must receive generated 6-digit PIN", pin)

        val verifyResp = c.verifyPairing("phone-123", pin)
        assertEquals(PairingStatus.APPROVED, verifyResp.status)
        assertNotNull("Session token must be issued upon approval", verifyResp.sessionToken)
        assertEquals(ConnectionState.CONNECTED, c.connectionState.value)
        assertTrue(s.isDevicePaired("phone-123"))

        val cmd = RemoteCommand(type = RemoteCommandType.DPAD_UP)
        val cmdResult = c.sendCommand(cmd)
        assertTrue("Command should execute successfully", cmdResult.success)
        assertEquals("Executed DPAD_UP on TV", cmdResult.message)
    }

    @Test
    fun testRejectedPairingAtSecurityGate() = runBlocking {
        val s = server ?: throw IllegalStateException("Server not initialized")
        val c = client ?: throw IllegalStateException("Client not initialized")

        s.securityGateCallback = { _, _, _, _, onReject ->
            onReject()
        }

        val connected = c.connect("127.0.0.1", s.actualPort)
        assertTrue(connected)

        val pairResp = c.requestPairing("phone-unauthorized", "Unknown Phone")
        assertEquals(PairingStatus.REJECTED, pairResp.status)
        assertEquals(ConnectionState.NOT_PAIRED, c.connectionState.value)
    }

    @Test
    fun testInvalidPairingPin() = runBlocking {
        val s = server ?: throw IllegalStateException("Server not initialized")
        val c = client ?: throw IllegalStateException("Client not initialized")

        s.securityGateCallback = { _, _, _, onApprove, _ ->
            onApprove()
        }

        val connected = c.connect("127.0.0.1", s.actualPort)
        assertTrue(connected)

        val pairResp = c.requestPairing("phone-123", "Samsung Galaxy F54")
        assertEquals(PairingStatus.PENDING_APPROVAL, pairResp.status)

        val verifyResp = c.verifyPairing("phone-123", "000000_WRONG_PIN")
        assertEquals(PairingStatus.INVALID_PIN, verifyResp.status)
        assertEquals(ConnectionState.NOT_PAIRED, c.connectionState.value)
    }

    @Test
    fun testHandshakeAuthenticationWithSessionToken() = runBlocking {
        val s = server ?: throw IllegalStateException("Server not initialized")
        val c = client ?: throw IllegalStateException("Client not initialized")

        val knownToken = "valid-session-token-999"
        s.pairedDevices["phone-f54"] = PairedDevice(
            deviceId = "phone-f54",
            deviceName = "Samsung Galaxy F54",
            sessionToken = knownToken
        )

        val connected = c.connect("127.0.0.1", s.actualPort)
        assertTrue(connected)

        val handshake = c.authenticate("phone-f54", knownToken, "Samsung Galaxy F54")
        assertEquals(HandshakeStatus.SUCCESS, handshake.status)
        assertEquals(ConnectionState.CONNECTED, c.connectionState.value)
    }

    @Test
    fun testRevokedDeviceAuthenticationAndCommandRejection() = runBlocking {
        val s = server ?: throw IllegalStateException("Server not initialized")
        val c = client ?: throw IllegalStateException("Client not initialized")

        val knownToken = "token-revoked"
        s.pairedDevices["phone-revoked"] = PairedDevice(
            deviceId = "phone-revoked",
            deviceName = "Stolen Phone",
            sessionToken = knownToken
        )
        s.revokeDevice("phone-revoked")

        val connected = c.connect("127.0.0.1", s.actualPort)
        assertTrue(connected)

        val handshake = c.authenticate("phone-revoked", knownToken)
        assertEquals(HandshakeStatus.UNAUTHORIZED, handshake.status)
        assertEquals(ConnectionState.AUTH_FAILED, c.connectionState.value)

        val cmdResult = c.sendCommand(RemoteCommand(type = RemoteCommandType.HOME), sessionToken = knownToken)
        assertFalse(cmdResult.success)
    }

    @Test
    fun testExpiredSessionRejection() = runBlocking {
        val s = server ?: throw IllegalStateException("Server not initialized")
        val c = client ?: throw IllegalStateException("Client not initialized")

        val expiredToken = "token-expired"
        s.pairedDevices["phone-expired"] = PairedDevice(
            deviceId = "phone-expired",
            deviceName = "Old Phone",
            sessionToken = expiredToken,
            expiresAt = System.currentTimeMillis() - 10000L
        )

        val connected = c.connect("127.0.0.1", s.actualPort)
        assertTrue(connected)

        val handshake = c.authenticate("phone-expired", expiredToken)
        assertEquals(HandshakeStatus.EXPIRED, handshake.status)
        assertEquals(ConnectionState.AUTH_FAILED, c.connectionState.value)
    }

    @Test
    fun testHeartbeatPingPong() = runBlocking {
        val s = server ?: throw IllegalStateException("Server not initialized")
        val c = client ?: throw IllegalStateException("Client not initialized")

        val connected = c.connect("127.0.0.1", s.actualPort)
        assertTrue(connected)

        val pingOk = c.ping()
        assertTrue("Ping should succeed with pong response", pingOk)
    }

    @Test
    fun testReconnectionFlow() = runBlocking {
        val s = server ?: throw IllegalStateException("Server not initialized")
        val c = client ?: throw IllegalStateException("Client not initialized")

        var correctPin = ""
        s.securityGateCallback = { _, _, pin, onApprove, _ ->
            correctPin = pin
            onApprove()
        }

        c.connect("127.0.0.1", s.actualPort)
        c.requestPairing("phone-reconnect", "Samsung Phone")
        c.verifyPairing("phone-reconnect", correctPin)
        assertTrue(c.isConnected())

        // Simulate disconnect
        c.disconnect()
        assertFalse(c.isConnected())
        assertEquals(ConnectionState.DISCONNECTED, c.connectionState.value)

        // Reconnect using stored credentials
        val reconnected = c.reconnect()
        assertTrue("Reconnection should restore authenticated connection", reconnected)
        assertTrue(c.isConnected())
    }
}
