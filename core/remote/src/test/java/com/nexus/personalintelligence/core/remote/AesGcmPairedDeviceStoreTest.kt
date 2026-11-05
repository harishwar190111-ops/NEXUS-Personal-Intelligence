package com.nexus.personalintelligence.core.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.AEADBadTagException

class AesGcmPairedDeviceStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSaveAndReloadWithRandomIvAndGcm() {
        val file = File(tempFolder.root, "paired_devices_gcm.enc")
        val keyProvider = JvmDeviceKeyProvider()
        val store = AesGcmPairedDeviceStore(file, keyProvider)

        val devices = listOf(
            PairedDevice(
                deviceId = "phone-samsung-f54",
                deviceName = "Galaxy F54",
                sessionToken = "super-secret-session-token-xyz-123",
                pairedAt = 1000L,
                lastSeenAt = 2000L
            ),
            PairedDevice(
                deviceId = "tablet-tab-s8",
                deviceName = "Galaxy Tab S8",
                sessionToken = "secret-token-456",
                pairedAt = 3000L,
                lastSeenAt = 4000L,
                isRevoked = true
            )
        )

        store.saveDevices(devices)
        assertTrue("Storage file should exist on disk", file.exists())

        // Ensure token is not in plaintext in the encrypted file
        val rawContent = file.readText()
        assertFalse(rawContent.contains("super-secret-session-token-xyz-123"))
        assertFalse(rawContent.contains("Galaxy F54"))

        // Create new store instance pointing to same file and same key
        val reloadedStore = AesGcmPairedDeviceStore(file, keyProvider)
        val loaded = reloadedStore.loadDevices()

        assertEquals(2, loaded.size)
        val dev1 = loaded.find { it.deviceId == "phone-samsung-f54" }
        assertEquals("Galaxy F54", dev1?.deviceName)
        assertEquals("super-secret-session-token-xyz-123", dev1?.sessionToken)
        assertFalse(dev1?.isRevoked ?: true)

        val dev2 = loaded.find { it.deviceId == "tablet-tab-s8" }
        assertTrue(dev2?.isRevoked ?: false)
    }

    @Test
    fun testTwoSavesProduceDifferentCiphertextsDueToRandomIv() {
        val file1 = File(tempFolder.root, "save1.enc")
        val file2 = File(tempFolder.root, "save2.enc")
        val keyProvider = JvmDeviceKeyProvider()

        val store1 = AesGcmPairedDeviceStore(file1, keyProvider)
        val store2 = AesGcmPairedDeviceStore(file2, keyProvider)

        val devices = listOf(
            PairedDevice(deviceId = "dev-1", deviceName = "Dev", sessionToken = "tok")
        )

        store1.saveDevices(devices)
        store2.saveDevices(devices)

        val text1 = file1.readText()
        val text2 = file2.readText()

        assertNotEquals("Each save must use a fresh random IV, producing distinct ciphertexts", text1, text2)
    }

    @Test
    fun testTamperedCiphertextFailsAuthenticatedDecryption() {
        val file = File(tempFolder.root, "tampered.enc")
        val keyProvider = JvmDeviceKeyProvider()
        val store = AesGcmPairedDeviceStore(file, keyProvider)

        store.saveDevices(listOf(PairedDevice(deviceId = "dev", deviceName = "D", sessionToken = "tok")))

        // Tamper with file bytes
        val encoded = file.readText().trim()
        val bytes = java.util.Base64.getDecoder().decode(encoded)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0xFF).toByte()
        file.writeText(java.util.Base64.getEncoder().encodeToString(bytes))

        assertThrows(Exception::class.java) {
            store.loadDevices()
        }
    }

    @Test
    fun testWrongKeyThrowsException() {
        val file = File(tempFolder.root, "wrong_key.enc")
        val store1 = AesGcmPairedDeviceStore(file, JvmDeviceKeyProvider())
        store1.saveDevices(listOf(PairedDevice(deviceId = "dev", deviceName = "D", sessionToken = "tok")))

        val storeDifferentKey = AesGcmPairedDeviceStore(file, JvmDeviceKeyProvider())
        assertThrows(Exception::class.java) {
            storeDifferentKey.loadDevices()
        }
    }

    @Test
    fun testSaveDevicesHandlesIoExceptionWithoutThrowing() {
        val readOnlyDir = File(tempFolder.root, "readonly_dir")
        readOnlyDir.mkdirs()
        readOnlyDir.setReadOnly()
        val impossibleFile = File(readOnlyDir, "cannot_write.enc")
        val store = AesGcmPairedDeviceStore(impossibleFile, JvmDeviceKeyProvider())

        store.saveDevices(listOf(PairedDevice(deviceId = "d1", deviceName = "D1", sessionToken = "tok")))
        assertTrue("Store must report lastSaveError when write fails", store.lastSaveError != null)
    }

    @Test
    fun testSaveDevicesHandlesKeystoreExceptionWithoutThrowing() {
        val failingProvider = object : DeviceKeyProvider {
            override fun getOrCreateKey(): javax.crypto.SecretKey {
                throw java.security.KeyStoreException("Android Keystore unavailable or locked")
            }
        }
        val file = File(tempFolder.root, "keystore_fail.enc")
        val store = AesGcmPairedDeviceStore(file, failingProvider)

        store.saveDevices(listOf(PairedDevice(deviceId = "d1", deviceName = "D1", sessionToken = "tok")))
        assertTrue("Store must report lastSaveError when Keystore fails", store.lastSaveError != null)
        assertTrue(store.lastSaveError!!.contains("Android Keystore unavailable"))
    }
}
