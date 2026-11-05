package com.nexus.personalintelligence.tv.network

import com.nexus.personalintelligence.core.remote.AesGcmPairedDeviceStore
import com.nexus.personalintelligence.core.remote.DeviceKeyProvider
import com.nexus.personalintelligence.core.remote.NexusSocketClient
import com.nexus.personalintelligence.core.remote.NexusSocketServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class TvPairedDeviceRestoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class StableTestKeyProvider : DeviceKeyProvider {
        private val key: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        override fun getOrCreateKey(): SecretKey = key
    }

    @Test
    fun testPairedDevicesPersistedAndRestoredAcrossRestart() = runBlocking {
        val storageFile = File(tempFolder.root, "tv_paired_devices.enc")
        val keyProvider = StableTestKeyProvider()
        val realStore = AesGcmPairedDeviceStore(storageFile, keyProvider)

        // Phase 1: Server 1 starts
        val server1 = NexusSocketServer(
            port = 0,
            tvDeviceName = "Living Room TV",
            pairedDeviceStore = realStore
        )
        server1.start()

        val client = NexusSocketClient()
        var serverPin = ""
        server1.securityGateCallback = { _, _, pin, onApprove, _ ->
            serverPin = pin
            onApprove()
        }

        client.connect("127.0.0.1", server1.actualPort)
        client.requestPairing("phone-dev-1", "Samsung Galaxy F54")
        val verifyResp = client.verifyPairing("phone-dev-1", serverPin)
        assertTrue(verifyResp.sessionToken != null)
        assertTrue(server1.isDevicePaired("phone-dev-1"))

        // Shutdown Server 1
        client.disconnect()
        server1.stop()
        assertTrue("Encrypted file must exist on disk after pairing", storageFile.exists())

        // Phase 2: TV Restarts (Server 2 instantiated with real store and same file)
        val server2 = NexusSocketServer(
            port = 0,
            tvDeviceName = "Living Room TV",
            pairedDeviceStore = AesGcmPairedDeviceStore(storageFile, keyProvider)
        )
        server2.start()

        // Verify Server 2 loaded the paired device on startup
        assertTrue("Device must be restored on TV restart", server2.isDevicePaired("phone-dev-1"))
        val restoredDevice = server2.getPairedDevice("phone-dev-1")
        assertNotNull(restoredDevice)
        assertEquals("Samsung Galaxy F54", restoredDevice?.deviceName)
        assertEquals(verifyResp.sessionToken, restoredDevice?.sessionToken)

        server2.stop()
    }

    @Test
    fun testCorruptFileStartsEmptyAndDoesNotCrash() {
        val storageFile = File(tempFolder.root, "corrupt_devices.enc")
        storageFile.writeText("corrupt_garbage_not_valid_gcm_ciphertext")

        val keyProvider = StableTestKeyProvider()
        val realStore = AesGcmPairedDeviceStore(storageFile, keyProvider)

        // Starting server with corrupt file should catch exception, report, and start empty
        val server = NexusSocketServer(
            port = 0,
            tvDeviceName = "Living Room TV",
            pairedDeviceStore = realStore
        )
        server.start()

        assertNotNull("Server must report last store load error", server.lastStoreLoadError)
        assertTrue(server.lastStoreLoadError!!.contains("Failed to load"))
        assertTrue("Server must start empty upon encountering corrupt file", server.getPairedDevices().isEmpty())
        assertFalse(server.isDevicePaired("any-device"))

        server.stop()
    }
}
