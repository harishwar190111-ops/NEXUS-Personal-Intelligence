package com.nexus.personalintelligence.core.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface DeviceKeyProvider {
    fun getOrCreateKey(): SecretKey
}

class JvmDeviceKeyProvider(
    private val key: SecretKey = generateKey()
) : DeviceKeyProvider {
    override fun getOrCreateKey(): SecretKey = key

    companion object {
        fun generateKey(): SecretKey {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            return keyGen.generateKey()
        }
    }
}

interface PairedDeviceStore {
    fun saveDevices(devices: List<PairedDevice>)
    fun loadDevices(): List<PairedDevice>
}

class AesGcmPairedDeviceStore(
    private val file: File,
    private val keyProvider: DeviceKeyProvider
) : PairedDeviceStore {

    private val json = Json { ignoreUnknownKeys = true }
    private val secureRandom = SecureRandom()

    var lastSaveError: String? = null
        private set
    var lastLoadError: String? = null
        private set

    companion object {
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    @Synchronized
    override fun saveDevices(devices: List<PairedDevice>) {
        try {
            val key = keyProvider.getOrCreateKey()
            val jsonText = json.encodeToString(devices)
            val plaintext = jsonText.toByteArray(StandardCharsets.UTF_8)

            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val ciphertext = cipher.doFinal(plaintext)

            // Store [IV (12 bytes)][Ciphertext + GCM Tag]
            val buffer = ByteBuffer.allocate(iv.size + ciphertext.size)
            buffer.put(iv)
            buffer.put(ciphertext)

            val encoded = Base64.getEncoder().encodeToString(buffer.array())
            file.parentFile?.mkdirs()
            file.writeText(encoded)
            lastSaveError = null
        } catch (e: Exception) {
            lastSaveError = "Failed to save paired devices: ${e.message}"
        }
    }

    @Synchronized
    override fun loadDevices(): List<PairedDevice> {
        if (!file.exists()) return emptyList()
        val encoded = file.readText().trim()
        if (encoded.isBlank()) return emptyList()

        val key = keyProvider.getOrCreateKey()
        val combined = Base64.getDecoder().decode(encoded)
        if (combined.size < GCM_IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Stored payload too short for AES-GCM IV")
        }

        val buffer = ByteBuffer.wrap(combined)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        buffer.get(iv)

        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val decryptedBytes = cipher.doFinal(ciphertext)
        val jsonText = String(decryptedBytes, StandardCharsets.UTF_8)

        return json.decodeFromString<List<PairedDevice>>(jsonText)
    }
}
