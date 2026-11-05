package com.nexus.personalintelligence.mobile.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SecretKeyProvider {
    fun getKey(): SecretKey
}

object AndroidKeyStoreSecretKeyProvider : SecretKeyProvider {
    private const val ALIAS = "nexus_provider_key"

    override fun getKey(): SecretKey {
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
}

/** Encrypts secrets (API keys, session tokens) with an AES-GCM key. */
open class SecretCipherEngine(private val keyProvider: SecretKeyProvider = AndroidKeyStoreSecretKeyProvider) {
    private val transform = "AES/GCM/NoPadding"

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(transform)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider.getKey())
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
            val cipher = Cipher.getInstance(transform)
            cipher.init(Cipher.DECRYPT_MODE, keyProvider.getKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        }
    } catch (_: Exception) {
        null
    }
}

object SecretCipher : SecretCipherEngine(AndroidKeyStoreSecretKeyProvider)
