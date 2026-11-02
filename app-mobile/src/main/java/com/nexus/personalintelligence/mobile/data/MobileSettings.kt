package com.nexus.personalintelligence.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexus.personalintelligence.mobile.ai.ProviderDraft
import com.nexus.personalintelligence.mobile.ai.ProviderSettings
import com.nexus.personalintelligence.mobile.ai.ProviderType
import com.nexus.personalintelligence.mobile.ai.ProviderValidator
import com.nexus.personalintelligence.mobile.security.SecretCipher
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class AssistantLanguage(val id: String, val label: String) {
    English("en", "English"),
    Tamil("ta", "Tamil"),
    Tanglish("tanglish", "Tanglish");

    companion object {
        fun fromId(id: String?): AssistantLanguage = AssistantLanguage.entries.firstOrNull { it.id == id } ?: English
    }
}

data class MobileSettings(
    val displayName: String = "",
    val language: AssistantLanguage = AssistantLanguage.English,
    val provider: ProviderSettings = ProviderSettings(),
)

private val Context.mobileStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_mobile_settings")

class MobileSettingsRepository(private val context: Context) {
    private object Keys {
        val NAME = stringPreferencesKey("display_name")
        val LANGUAGE = stringPreferencesKey("language")
        val PROVIDER = stringPreferencesKey("provider_type")
        val BASE_URL = stringPreferencesKey("provider_base_url")
        val MODEL = stringPreferencesKey("provider_model")
        val KEY_CIPHER = stringPreferencesKey("provider_key_cipher")
    }

    val settings: Flow<MobileSettings> = context.mobileStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val type = ProviderType.fromId(p[Keys.PROVIDER])
            MobileSettings(
                displayName = p[Keys.NAME].orEmpty(),
                language = AssistantLanguage.fromId(p[Keys.LANGUAGE]),
                provider = ProviderSettings(
                    type = type,
                    baseUrl = p[Keys.BASE_URL] ?: type.defaultBaseUrl,
                    model = p[Keys.MODEL].orEmpty(),
                    hasApiKey = p[Keys.KEY_CIPHER] != null,
                ),
            )
        }

    suspend fun setDisplayName(name: String) {
        context.mobileStore.edit { p -> p[Keys.NAME] = name }
    }

    suspend fun setLanguage(language: AssistantLanguage) {
        context.mobileStore.edit { p -> p[Keys.LANGUAGE] = language.id }
    }

    /** Returns null on success, otherwise a message to show the user. */
    suspend fun saveProvider(draft: ProviderDraft): String? {
        val errors = ProviderValidator.validate(draft)
        if (errors.isNotEmpty()) return errors.first()
        var cipherText: String? = null
        if (draft.apiKey.isNotBlank()) {
            cipherText = try {
                SecretCipher.encrypt(draft.apiKey.trim())
            } catch (e: Exception) {
                return "Could not store the API key securely on this phone."
            }
        }
        context.mobileStore.edit { p ->
            p[Keys.PROVIDER] = draft.type.id
            p[Keys.BASE_URL] = draft.baseUrl.trim()
            p[Keys.MODEL] = draft.model.trim()
            if (cipherText != null) p[Keys.KEY_CIPHER] = cipherText
        }
        return null
    }

    suspend fun removeApiKey() {
        context.mobileStore.edit { p -> p.remove(Keys.KEY_CIPHER) }
    }
}
