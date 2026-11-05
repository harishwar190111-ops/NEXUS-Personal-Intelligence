package com.nexus.personalintelligence.mobile.data

import com.nexus.personalintelligence.mobile.security.SecretCipher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator

class MobileTokenCipherTest {

    @Before
    fun setUp() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        SecretCipher.testSecretKey = keyGen.generateKey()
    }

    @After
    fun tearDown() {
        SecretCipher.testSecretKey = null
    }

    @Test
    fun testExplicitVersionPrefix() {
        val rawToken = "nexus-session-token-123456"
        val encResult = MobileTokenCipher.encryptToken(rawToken)

        assertTrue(encResult.isSuccess)
        val encrypted = encResult.getOrThrow()
        assertTrue("Stored token must have explicit enc:v1: prefix", encrypted.startsWith("enc:v1:"))
        assertFalse("Ciphertext must not expose plaintext token", encrypted.contains(rawToken))
    }

    @Test
    fun testEncryptAndDecryptRoundtrip() {
        val rawToken = "super-secret-auth-token-777"
        val encrypted = MobileTokenCipher.encryptToken(rawToken).getOrThrow()

        val decrypted = MobileTokenCipher.decryptToken(encrypted)
        assertEquals(rawToken, decrypted)
    }

    @Test
    fun testCorruptValueMeansUnpairedState() {
        // Tampered ciphertext
        val corruptCipher = "enc:v1:AAAA_NOT_VALID_BASE64:BBBB"
        val result = MobileTokenCipher.decryptToken(corruptCipher)
        assertNull("Corrupt ciphertext must return null (unpaired state), never a usable token", result)

        val garbageString = "broken_corrupt_data_without_prefix!!!"
        val garbageResult = MobileTokenCipher.decryptToken(garbageString)
        assertNull(garbageResult)
    }

    @Test
    fun testLegacyPlaintextTokenMigratesOnce() {
        val legacyPlaintext = "11112222-3333-4444-5555-666677778888"
        val decrypted = MobileTokenCipher.decryptToken(legacyPlaintext)
        assertEquals("Legacy plaintext token must be accepted once for migration", legacyPlaintext, decrypted)

        // Then re-encrypting it migrates it with the new version prefix
        val migrated = MobileTokenCipher.encryptToken(decrypted!!).getOrThrow()
        assertTrue(migrated.startsWith("enc:v1:"))
        assertEquals(legacyPlaintext, MobileTokenCipher.decryptToken(migrated))
    }

    @Test
    fun testLegacyColonFormatWithoutPrefixSupported() {
        // Encrypt with raw SecretCipher (produces iv:ciphertext without enc:v1:)
        val rawSecret = SecretCipher.encrypt("legacy-iv-ciphertext-token")
        assertFalse(rawSecret.startsWith("enc:v1:"))
        assertTrue(rawSecret.contains(":"))

        val decrypted = MobileTokenCipher.decryptToken(rawSecret)
        assertEquals("legacy-iv-ciphertext-token", decrypted)
    }
}
