package com.nexus.personalintelligence.core.ai

import com.nexus.personalintelligence.core.common.NexusResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
enum class AiProviderType {
    GEMINI, OPENAI, GROK, LOCAL_CUSTOM
}

@Serializable
data class AiProviderConfig(
    val providerType: AiProviderType = AiProviderType.GEMINI,
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelName: String = "",
    val isEnabled: Boolean = false
)

@Serializable
enum class IntentAction {
    OPEN_APP, CHANGE_THEME, CHANGE_BACKGROUND, NAVIGATE,
    VOLUME_CONTROL, MEDIA_CONTROL, SEARCH_WEBSITE,
    CONNECT_TV, DISCONNECT_TV, UNKNOWN
}

@Serializable
data class RecognizedAiIntent(
    val action: IntentAction,
    val target: String = "",
    val parameter: String = "",
    val originalText: String,
    val language: String = "en",
    val confidence: Float = 1.0f
)

class NexusAiIntentParser {
    fun parse(rawText: String): RecognizedAiIntent {
        val trimmed = rawText.trim().lowercase()
        val isTamilOrTanglish = trimmed.any { it in '\u0B80'..'\u0BFF' } || listOf("pannu", "ethu", "kura").any { trimmed.contains(it) }
        val lang = if (isTamilOrTanglish) "ta/tanglish" else "en"

        if (trimmed.contains("youtube") || trimmed.contains("யூடியூப்")) {
            return RecognizedAiIntent(IntentAction.OPEN_APP, target = "YouTube", parameter = "com.google.android.youtube.tv", originalText = rawText, language = lang)
        }
        if (trimmed.contains("netflix") || trimmed.contains("நெட்ஃபிக்ஸ்")) {
            return RecognizedAiIntent(IntentAction.OPEN_APP, target = "Netflix", parameter = "com.netflix.ninja", originalText = rawText, language = lang)
        }
        if (trimmed.contains("theme") || trimmed.contains("தீம்")) {
            val theme = if (trimmed.contains("aurora")) "Aurora" else "Liquid Glass"
            return RecognizedAiIntent(IntentAction.CHANGE_THEME, parameter = theme, originalText = rawText, language = lang)
        }
        if (trimmed.contains("volume") || trimmed.contains("ethu") || trimmed.contains("சத்தம்")) {
            return RecognizedAiIntent(IntentAction.VOLUME_CONTROL, parameter = "UP", originalText = rawText, language = lang)
        }
        return RecognizedAiIntent(IntentAction.UNKNOWN, originalText = rawText, language = lang, confidence = 0.5f)
    }
}

class NexusAiEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val intentParser: NexusAiIntentParser = NexusAiIntentParser()
) {
    fun parseDeterministicIntent(text: String): RecognizedAiIntent = intentParser.parse(text)

    suspend fun queryProvider(prompt: String, config: AiProviderConfig): NexusResult<String> {
        if (!config.isEnabled || config.apiKey.isBlank()) {
            val localIntent = intentParser.parse(prompt)
            return NexusResult.Success(
                "Deterministic Engine [Local Mode]: Understood action '${localIntent.action}' (target: ${localIntent.target.ifEmpty { localIntent.parameter }}). Configure API key in settings for generative responses."
            )
        }

        return try {
            when (config.providerType) {
                AiProviderType.OPENAI, AiProviderType.GROK, AiProviderType.LOCAL_CUSTOM -> {
                    val endpoint = if (config.baseUrl.isNotBlank()) config.baseUrl else "https://api.openai.com/v1/chat/completions"
                    val model = if (config.modelName.isNotBlank()) config.modelName else "gpt-3.5-turbo"
                    val jsonBody = """
                        {
                            "model": "$model",
                            "messages": [{"role": "user", "content": ${Json.encodeToString(kotlinx.serialization.serializer(), prompt)}}]
                        }
                    """.trimIndent()
                    val req = Request.Builder()
                        .url(endpoint)
                        .header("Authorization", "Bearer ${config.apiKey}")
                        .post(jsonBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    val resp = client.newCall(req).execute()
                    val body = resp.body?.string() ?: ""
                    if (resp.isSuccessful) {
                        NexusResult.Success("Response received: $body")
                    } else {
                        NexusResult.Error("API error (${resp.code}): $body")
                    }
                }
                AiProviderType.GEMINI -> {
                    val model = if (config.modelName.isNotBlank()) config.modelName else "gemini-1.5-flash"
                    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${config.apiKey}"
                    val jsonBody = """
                        {
                            "contents": [{"parts": [{"text": ${Json.encodeToString(kotlinx.serialization.serializer(), prompt)}}]}]
                        }
                    """.trimIndent()
                    val req = Request.Builder()
                        .url(endpoint)
                        .post(jsonBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    val resp = client.newCall(req).execute()
                    val body = resp.body?.string() ?: ""
                    if (resp.isSuccessful) {
                        NexusResult.Success("Gemini response: $body")
                    } else {
                        NexusResult.Error("Gemini error (${resp.code}): $body")
                    }
                }
            }
        } catch (e: Exception) {
            NexusResult.Error("AI Provider error: ${e.message}", e)
        }
    }
}
