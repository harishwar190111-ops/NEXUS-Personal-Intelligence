package com.nexus.personalintelligence.mobile.ai

import java.net.URI

/** Provider-agnostic configuration. No provider code exists yet: this only stores and validates settings. */
enum class ProviderType(
    val id: String,
    val label: String,
    val defaultBaseUrl: String,
    val needsKey: Boolean,
    val allowHttp: Boolean,
) {
    NONE("none", "None - local commands only", "", false, false),
    OPENAI_COMPATIBLE("openai", "OpenAI-compatible", "https://api.openai.com/v1", true, false),
    GEMINI("gemini", "Google Gemini (OpenAI-compatible endpoint)", "https://generativelanguage.googleapis.com/v1beta/openai/", true, false),
    ANTHROPIC("anthropic", "Anthropic Claude", "https://api.anthropic.com", true, false),
    LOCAL_SERVER("local", "Local server (Ollama / LM Studio)", "http://192.168.1.10:11434/v1", false, true);

    companion object {
        fun fromId(id: String?): ProviderType = ProviderType.entries.firstOrNull { it.id == id } ?: NONE
    }
}

data class ProviderSettings(
    val type: ProviderType = ProviderType.NONE,
    val baseUrl: String = "",
    val model: String = "",
    val hasApiKey: Boolean = false,
)

data class ProviderDraft(
    val type: ProviderType,
    val baseUrl: String,
    val model: String,
    val apiKey: String,
    val hasStoredKey: Boolean,
)

object ProviderValidator {
    /** Returns human-readable problems; empty list = valid. */
    fun validate(d: ProviderDraft): List<String> {
        if (d.type == ProviderType.NONE) return emptyList()
        val errors = mutableListOf<String>()
        val uri = try {
            URI(d.baseUrl.trim())
        } catch (e: Exception) {
            null
        }
        val scheme = uri?.scheme?.lowercase()
        val hostOk = !uri?.host.isNullOrBlank()
        val schemeOk = scheme == "https" || (scheme == "http" && d.type.allowHttp)
        if (uri == null || !hostOk || !schemeOk) {
            errors += if (d.type.allowHttp) "Enter a valid http:// or https:// address." else "Enter a valid https:// address."
        }
        if (d.model.isBlank()) errors += "Enter the model name."
        if (d.type.needsKey && d.apiKey.isBlank() && !d.hasStoredKey) errors += "An API key is required."
        return errors
    }
}

/** Honest one-line status. Nothing is ever reported as "connected": Release 1 makes no network requests. */
fun providerStatus(p: ProviderSettings): String = when {
    p.type == ProviderType.NONE -> "No AI provider selected. NEXUS answers with local commands only."
    p.baseUrl.isBlank() || p.model.isBlank() || (p.type.needsKey && !p.hasApiKey) -> "${p.type.label}: setup incomplete."
    else -> "${p.type.label}: saved, not verified. This build never contacts the provider."
}
