package com.nexus.personalintelligence.mobile.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderValidatorTest {
    private fun draft(
        type: ProviderType,
        url: String = type.defaultBaseUrl,
        model: String = "m",
        key: String = "k",
        stored: Boolean = false,
    ) = ProviderDraft(type, url, model, key, stored)

    @Test
    fun none_isAlwaysValid() {
        assertTrue(ProviderValidator.validate(draft(ProviderType.NONE, url = "", model = "", key = "")).isEmpty())
    }

    @Test
    fun cloudProvider_needsHttpsModelAndKey() {
        assertTrue(ProviderValidator.validate(draft(ProviderType.OPENAI_COMPATIBLE)).isEmpty())
        assertEquals(1, ProviderValidator.validate(draft(ProviderType.OPENAI_COMPATIBLE, url = "http://api.example.com")).size)
        assertEquals(1, ProviderValidator.validate(draft(ProviderType.OPENAI_COMPATIBLE, model = " ")).size)
        assertEquals(1, ProviderValidator.validate(draft(ProviderType.OPENAI_COMPATIBLE, key = "")).size)
        assertTrue(ProviderValidator.validate(draft(ProviderType.OPENAI_COMPATIBLE, key = "", stored = true)).isEmpty())
    }

    @Test
    fun localServer_allowsHttpAndNoKey() {
        assertTrue(ProviderValidator.validate(draft(ProviderType.LOCAL_SERVER, key = "")).isEmpty())
    }

    @Test
    fun garbageUrl_isRejected() {
        assertEquals(1, ProviderValidator.validate(draft(ProviderType.GEMINI, url = "not a url")).size)
        assertEquals(1, ProviderValidator.validate(draft(ProviderType.GEMINI, url = "")).size)
    }

    @Test
    fun status_neverClaimsConnected() {
        assertTrue(providerStatus(ProviderSettings()).contains("local commands only"))
        val saved = ProviderSettings(ProviderType.GEMINI, "https://x.example", "m", true)
        assertTrue(providerStatus(saved).contains("not verified"))
        assertTrue(providerStatus(saved.copy(hasApiKey = false)).contains("incomplete"))
    }
}
