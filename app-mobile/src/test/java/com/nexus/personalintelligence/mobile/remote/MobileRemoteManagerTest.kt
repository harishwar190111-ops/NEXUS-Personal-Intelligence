package com.nexus.personalintelligence.mobile.remote

import com.nexus.personalintelligence.core.remote.CommandResult
import com.nexus.personalintelligence.core.remote.ConnectionState
import com.nexus.personalintelligence.core.remote.NexusSocketClient
import com.nexus.personalintelligence.core.remote.NexusSocketServer
import com.nexus.personalintelligence.core.remote.PairingStatus
import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import com.nexus.personalintelligence.mobile.data.PairedTvInfo
import com.nexus.personalintelligence.mobile.data.PairedTvStorage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class MobileRemoteManagerTest {

    private var server: NexusSocketServer? = null
    private var client: NexusSocketClient? = null
    private var remoteManager: MobileRemoteManager? = null
    private val fakeStorage = FakePairedTvStorage()
    private val receivedCommands = mutableListOf<RemoteCommand>()

    class FakePairedTvStorage : PairedTvStorage {
        var savedInfo: PairedTvInfo? = null

        override suspend fun savePairedTv(info: PairedTvInfo) {
            savedInfo = info
        }

        override suspend fun clearPairedTv() {
            savedInfo = null
        }
    }

    @Before
    fun setUp() {
        server = NexusSocketServer(
            port = 0,
            tvDeviceName = "Motorola TV Stick",
            commandDispatcher = { cmd ->
                receivedCommands.add(cmd)
                CommandResult(commandId = cmd.commandId, success = true, message = "Executed ${cmd.type}")
            }
        )
        server?.start()

        client = NexusSocketClient()
        remoteManager = MobileRemoteManager(
            storage = fakeStorage,
            client = client!!
        )
    }

    @After
    fun tearDown() {
        remoteManager?.disconnect()
        server?.stop()
    }

    @Test
    fun testCompletePairingWorkflowAndStorage() = runBlocking {
        val s = server ?: throw IllegalStateException("server null")
        val mgr = remoteManager ?: throw IllegalStateException("mgr null")

        val capturedPin = AtomicReference<String>()
        s.securityGateCallback = { _, _, pin, onApprove, _ ->
            capturedPin.set(pin)
            onApprove()
        }

        // Start pairing
        val pairResp = mgr.startPairing("127.0.0.1", s.actualPort, "Samsung Galaxy F54")
        assertEquals(PairingStatus.PENDING_APPROVAL, pairResp.status)
        assertEquals(ConnectionState.AWAITING_TV_APPROVAL, mgr.connectionState.value)

        val pin = capturedPin.get()
        assertNotNull("Security review gate must receive generated PIN", pin)

        // Verify PIN
        val verifyResp = mgr.verifyPin(pin)
        assertEquals(PairingStatus.APPROVED, verifyResp.status)
        assertEquals(ConnectionState.CONNECTED, mgr.connectionState.value)

        // Verify stored in storage
        assertNotNull("Paired TV info must be persisted", fakeStorage.savedInfo)
        assertEquals("127.0.0.1", fakeStorage.savedInfo?.host)
        assertEquals(s.actualPort, fakeStorage.savedInfo?.port)
        assertTrue(fakeStorage.savedInfo?.isPaired == true)
    }

    @Test
    fun testInvalidPinRejection() = runBlocking {
        val s = server ?: throw IllegalStateException("server null")
        val mgr = remoteManager ?: throw IllegalStateException("mgr null")

        s.securityGateCallback = { _, _, _, onApprove, _ ->
            onApprove()
        }

        mgr.startPairing("127.0.0.1", s.actualPort, "Samsung Galaxy F54")
        val verifyResp = mgr.verifyPin("999999")
        assertEquals(PairingStatus.INVALID_PIN, verifyResp.status)
        assertNull("Storage must remain empty on invalid PIN", fakeStorage.savedInfo)
    }

    @Test
    fun testReconnectionUsingSavedCredentials() = runBlocking {
        val s = server ?: throw IllegalStateException("server null")
        val mgr = remoteManager ?: throw IllegalStateException("mgr null")

        var correctPin = ""
        s.securityGateCallback = { _, _, pin, onApprove, _ ->
            correctPin = pin
            onApprove()
        }

        mgr.startPairing("127.0.0.1", s.actualPort, "Samsung Galaxy F54")
        mgr.verifyPin(correctPin)
        assertEquals(ConnectionState.CONNECTED, mgr.connectionState.value)

        val saved = fakeStorage.savedInfo
        assertNotNull(saved)

        // Disconnect
        mgr.disconnect()
        assertFalse(mgr.connectionState.value == ConnectionState.CONNECTED)

        // Reconnect with saved credentials without re-entering PIN
        val reconnected = mgr.connectWithSaved(saved!!)
        assertTrue("Reconnection must succeed with session token", reconnected)
        assertEquals(ConnectionState.CONNECTED, mgr.connectionState.value)
    }

    @Test
    fun testSendRemoteCommandsAndTamilTextInput() = runBlocking {
        val s = server ?: throw IllegalStateException("server null")
        val mgr = remoteManager ?: throw IllegalStateException("mgr null")

        var correctPin = ""
        s.securityGateCallback = { _, _, pin, onApprove, _ ->
            correctPin = pin
            onApprove()
        }

        mgr.startPairing("127.0.0.1", s.actualPort, "Samsung Galaxy F54")
        mgr.verifyPin(correctPin)

        // D-Pad command
        val cmdDpad = RemoteCommand(type = RemoteCommandType.DPAD_UP)
        val resDpad = mgr.sendCommand(cmdDpad)
        assertTrue(resDpad.success)

        // Media command
        val cmdPlay = RemoteCommand(type = RemoteCommandType.PLAY_PAUSE)
        val resPlay = mgr.sendCommand(cmdPlay)
        assertTrue(resPlay.success)

        // Tamil Unicode text input
        val tamilWord = "வணக்கம்"
        val cmdTamil = RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = tamilWord)
        val resTamil = mgr.sendCommand(cmdTamil)
        assertTrue(resTamil.success)

        // Verify commands reached server
        assertEquals(3, receivedCommands.size)
        assertEquals(RemoteCommandType.DPAD_UP, receivedCommands[0].type)
        assertEquals(RemoteCommandType.PLAY_PAUSE, receivedCommands[1].type)
        assertEquals(RemoteCommandType.TEXT_INPUT, receivedCommands[2].type)
        assertEquals("வணக்கம்", receivedCommands[2].text)

        // Verify command history and event store tracking
        assertEquals(3, mgr.historyTracker.size())
        assertTrue("Event store should track pairing and command events", mgr.eventStore.size() >= 3)
    }

    @Test
    fun testUnpairClearsCredentials() = runBlocking {
        val s = server ?: throw IllegalStateException("server null")
        val mgr = remoteManager ?: throw IllegalStateException("mgr null")

        var correctPin = ""
        s.securityGateCallback = { _, _, pin, onApprove, _ ->
            correctPin = pin
            onApprove()
        }

        mgr.startPairing("127.0.0.1", s.actualPort, "Samsung Galaxy F54")
        mgr.verifyPin(correctPin)
        assertNotNull(fakeStorage.savedInfo)

        mgr.unpair()
        assertNull("Unpair must clear storage credentials", fakeStorage.savedInfo)
    }
}
