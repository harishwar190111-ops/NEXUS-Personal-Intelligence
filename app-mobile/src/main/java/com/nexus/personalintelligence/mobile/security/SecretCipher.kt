package com.nexus.personalintelligence.mobile.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts secrets (API keys, session tokens) with an AES-GCM key that never leaves the Android Keystore. */
object SecretCipher {
    private const val ALIAS = "nexus_provider_key"
    private const val TRANSFORM = "AES/GCM/NoPadding"

    var testSecretKey: SecretKey? = null

    private fun key(): SecretKey {
        testSecretKey?.let { return it }
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(cipher.iv) + ":" + encoder.encodeToString(bytes)
    }

    fun decrypt(stored: String): String? = try {
        val parts = stored.split(':')
        if (parts.size != 2) null
        else {
            val decoder = Base64.getDecoder()
            val iv = decoder.decode(parts[0])
            val data = decoder.decode(parts[1])
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        }
    } catch (_: Exception) {
        null
    }
}
