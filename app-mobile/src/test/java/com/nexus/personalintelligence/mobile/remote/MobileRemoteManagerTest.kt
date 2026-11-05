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
        var shouldFailSave: Boolean = false

        override suspend fun savePairedTv(info: PairedTvInfo): Result<Unit> {
            if (shouldFailSave) return Result.failure(IllegalStateException("Simulated storage encryption failure"))
            savedInfo = info
            return Result.success(Unit)
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
        assertEquals("Text input delivered", resTamil.message)
        assertFalse("Message must never contain typed text", resTamil.message.contains(tamilWord))

        // Verify commands reached server
        assertEquals(3, receivedCommands.size)
        assertEquals(RemoteCommandType.DPAD_UP, receivedCommands[0].type)
        assertEquals(RemoteCommandType.PLAY_PAUSE, receivedCommands[1].type)
        assertEquals(RemoteCommandType.TEXT_INPUT, receivedCommands[2].type)
        assertEquals("வணக்கம்", receivedCommands[2].text)

        // Verify command history and event store tracking
        assertEquals(3, mgr.historyTracker.size())
        assertFalse(mgr.historyTracker.getHistory().any { it.message.contains(tamilWord) })
        assertTrue("Event store should track pairing and command events", mgr.eventStore.listEvents().size >= 3)
    }

    @Test
    fun testTextInputFailurePreservesTruthfulFailureMessageAndNeverDelivered() = runBlocking {
        // Create server that rejects text input
        server?.stop()
        server = NexusSocketServer(
            port = 0,
            tvDeviceName = "Test TV",
            commandDispatcher = { cmd ->
                CommandResult(cmd.commandId, false, "No active text receiver on TV")
            }
        )
        server?.start()
        val s = server ?: throw IllegalStateException("server null")
        val mgr = remoteManager ?: throw IllegalStateException("mgr null")

        var correctPin = ""
        s.securityGateCallback = { _, _, pin, onApprove, _ ->
            correctPin = pin
            onApprove()
        }

        mgr.startPairing("127.0.0.1", s.actualPort, "Samsung Galaxy F54")
        mgr.verifyPin(correctPin)

        val cmd = RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = "secret-pass")
        val res = mgr.sendCommand(cmd)

        assertFalse("Command must indicate failure", res.success)
        assertEquals("No active text receiver on TV", res.message)
        assertFalse("Must never say delivered when failure occurs", res.message.contains("delivered"))
        assertFalse("Must never leak typed text in failure message", res.message.contains("secret-pass"))
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
