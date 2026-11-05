package com.nexus.personalintelligence.mobile.data

import com.nexus.personalintelligence.mobile.security.SecretCipherEngine
import com.nexus.personalintelligence.mobile.security.SecretKeyProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class MobileTokenCipherTest {

    private lateinit var cipherEngine: SecretCipherEngine
    private lateinit var tokenCipher: MobileTokenCipherEngine

    private class TestKeyProvider(private val secretKey: SecretKey) : SecretKeyProvider {
        override fun getKey(): SecretKey = secretKey
    }

    @Before
    fun setUp() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val key = keyGen.generateKey()
        cipherEngine = SecretCipherEngine(TestKeyProvider(key))
        tokenCipher = MobileTokenCipherEngine(cipherEngine)
    }

    @Test
    fun testExplicitVersionPrefix() {
        val rawToken = "nexus-session-token-123456"
        val encResult = tokenCipher.encryptToken(rawToken)

        assertTrue(encResult.isSuccess)
        val encrypted = encResult.getOrThrow()
        assertTrue("Stored token must have explicit enc:v1: prefix", encrypted.startsWith("enc:v1:"))
        assertFalse("Ciphertext must not expose plaintext token", encrypted.contains(rawToken))
    }

    @Test
    fun testEncryptAndDecryptRoundtrip() {
        val rawToken = "super-secret-auth-token-777"
        val encrypted = tokenCipher.encryptToken(rawToken).getOrThrow()

        val decrypted = tokenCipher.decryptToken(encrypted)
        assertEquals(rawToken, decrypted)
    }

    @Test
    fun testCorruptValueMeansUnpairedState() {
        // Tampered ciphertext
        val corruptCipher = "enc:v1:AAAA_NOT_VALID_BASE64:BBBB"
        val result = tokenCipher.decryptToken(corruptCipher)
        assertNull("Corrupt ciphertext must return null (unpaired state), never a usable token", result)

        val garbageString = "broken_corrupt_data_without_prefix!!!"
        val garbageResult = tokenCipher.decryptToken(garbageString)
        assertNull(garbageResult)
    }

    @Test
    fun testLegacyPlaintextTokenMigratesOnce() {
        val legacyPlaintext = "11112222-3333-4444-5555-666677778888"
        val decrypted = tokenCipher.decryptToken(legacyPlaintext)
        assertEquals("Legacy plaintext token must be accepted once for migration", legacyPlaintext, decrypted)

        // Then re-encrypting it migrates it with the new version prefix
        val migrated = tokenCipher.encryptToken(decrypted!!).getOrThrow()
        assertTrue(migrated.startsWith("enc:v1:"))
        assertEquals(legacyPlaintext, tokenCipher.decryptToken(migrated))
    }

    @Test
    fun testLegacyColonFormatWithoutPrefixSupported() {
        // Encrypt with raw cipherEngine (produces iv:ciphertext without enc:v1:)
        val rawSecret = cipherEngine.encrypt("legacy-iv-ciphertext-token")
        assertFalse(rawSecret.startsWith("enc:v1:"))
        assertTrue(rawSecret.contains(":"))

        val decrypted = tokenCipher.decryptToken(rawSecret)
        assertEquals("legacy-iv-ciphertext-token", decrypted)
    }

    @Test
    fun testIsLegacyFormatDetection() {
        assertTrue(tokenCipher.isLegacyFormat("11112222-3333-4444-5555-666677778888"))
        assertTrue(tokenCipher.isLegacyFormat("iv:ciphertext"))
        assertFalse(tokenCipher.isLegacyFormat("enc:v1:iv:ciphertext"))
        assertFalse(tokenCipher.isLegacyFormat("not-a-uuid"))
        assertFalse(tokenCipher.isLegacyFormat(""))
    }
}
