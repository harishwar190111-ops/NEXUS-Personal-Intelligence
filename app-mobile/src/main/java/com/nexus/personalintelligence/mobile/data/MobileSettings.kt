package com.nexus.personalintelligence.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexus.personalintelligence.mobile.ai.ProviderDraft
import com.nexus.personalintelligence.mobile.ai.ProviderSettings
import com.nexus.personalintelligence.mobile.ai.ProviderType
import com.nexus.personalintelligence.mobile.ai.ProviderValidator
import com.nexus.personalintelligence.mobile.security.SecretCipher
import com.nexus.personalintelligence.mobile.security.SecretCipherEngine
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class AssistantLanguage(val id: String, val label: String) {
    English("en", "English"),
    Tamil("ta", "Tamil"),
    Tanglish("tanglish", "Tanglish");

    companion object {
        fun fromId(id: String?): AssistantLanguage = AssistantLanguage.entries.firstOrNull { it.id == id } ?: English
    }
}

data class PairedTvInfo(
    val host: String = "",
    val port: Int = 8765,
    val tvDeviceName: String = "",
    val sessionToken: String = "",
    val deviceId: String = "",
    val pairedAt: Long = 0L
) {
    val isPaired: Boolean get() = sessionToken.isNotBlank() && host.isNotBlank()
}

open class MobileTokenCipherEngine(private val cipherEngine: SecretCipherEngine = SecretCipher) {
    val prefix = "enc:v1:"
    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun encryptToken(rawToken: String): Result<String> {
        if (rawToken.isBlank()) return Result.success("")
        return runCatching {
            val ciphertext = cipherEngine.encrypt(rawToken)
            "$prefix$ciphertext"
        }
    }

    fun decryptToken(storedValue: String): String? {
        if (storedValue.isBlank()) return ""
        if (storedValue.startsWith(prefix)) {
            val payload = storedValue.removePrefix(prefix)
            return runCatching { cipherEngine.decrypt(payload) }.getOrNull()
        }
        // Legacy unversioned format with iv:ciphertext
        if (storedValue.contains(":") && !storedValue.startsWith(prefix)) {
            return runCatching { cipherEngine.decrypt(storedValue) }.getOrNull()
        }
        // Legacy plaintext token: accept only the exact format the TV server actually generates (UUID)
        if (storedValue.length == 36 && uuidRegex.matches(storedValue)) {
            return storedValue
        }
        // Any other unprefixed or corrupt value means unpaired state
        return null
    }

    fun isLegacyFormat(storedValue: String): Boolean {
        if (storedValue.isBlank() || storedValue.startsWith(prefix)) return false
        return (storedValue.contains(":") && !storedValue.startsWith(prefix)) ||
               (storedValue.length == 36 && uuidRegex.matches(storedValue))
    }
}

object MobileTokenCipher : MobileTokenCipherEngine(SecretCipher) {
    const val PREFIX = "enc:v1:"
}

interface PairedTvStorage {
    suspend fun savePairedTv(info: PairedTvInfo): Result<Unit>
    suspend fun clearPairedTv()
}

data class MobileSettings(
    val displayName: String = "",
    val language: AssistantLanguage = AssistantLanguage.English,
    val provider: ProviderSettings = ProviderSettings(),
    val pairedTv: PairedTvInfo = PairedTvInfo(),
)

private val Context.mobileStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_mobile_settings")

class MobileSettingsRepository(
    private val context: Context,
    private val tokenCipher: MobileTokenCipherEngine = MobileTokenCipher,
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : PairedTvStorage {
    private object Keys {
        val NAME = stringPreferencesKey("display_name")
        val LANGUAGE = stringPreferencesKey("language")
        val PROVIDER = stringPreferencesKey("provider_type")
        val BASE_URL = stringPreferencesKey("provider_base_url")
        val MODEL = stringPreferencesKey("provider_model")
        val KEY_CIPHER = stringPreferencesKey("provider_key_cipher")
        val TV_HOST = stringPreferencesKey("tv_host")
        val TV_PORT = intPreferencesKey("tv_port")
        val TV_NAME = stringPreferencesKey("tv_name")
        val TV_SESSION_TOKEN = stringPreferencesKey("tv_session_token")
        val TV_DEVICE_ID = stringPreferencesKey("tv_device_id")
        val TV_PAIRED_AT = longPreferencesKey("tv_paired_at")
    }

    val settings: Flow<MobileSettings> = context.mobileStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val type = ProviderType.fromId(p[Keys.PROVIDER])
            val storedToken = p[Keys.TV_SESSION_TOKEN].orEmpty()
            val decryptedToken = tokenCipher.decryptToken(storedToken)
            val effectiveToken = decryptedToken ?: ""
            val isPaired = effectiveToken.isNotBlank() && !p[Keys.TV_HOST].isNullOrBlank()

            if (storedToken.isNotBlank() && tokenCipher.isLegacyFormat(storedToken) && effectiveToken.isNotBlank()) {
                // Re-encrypt legacy plaintext tokens on read
                ioScope.launch {
                    val reEncrypted = tokenCipher.encryptToken(effectiveToken).getOrNull()
                    if (reEncrypted != null) {
                        context.mobileStore.edit { prefs ->
                            if (prefs[Keys.TV_SESSION_TOKEN] == storedToken) {
                                prefs[Keys.TV_SESSION_TOKEN] = reEncrypted
                            }
                        }
                    }
                }
            }

            MobileSettings(
                displayName = p[Keys.NAME].orEmpty(),
                language = AssistantLanguage.fromId(p[Keys.LANGUAGE]),
                provider = ProviderSettings(
                    type = type,
                    baseUrl = p[Keys.BASE_URL] ?: type.defaultBaseUrl,
                    model = p[Keys.MODEL].orEmpty(),
                    hasApiKey = p[Keys.KEY_CIPHER] != null,
                ),
                pairedTv = PairedTvInfo(
                    host = if (isPaired) p[Keys.TV_HOST].orEmpty() else "",
                    port = p[Keys.TV_PORT] ?: 8765,
                    tvDeviceName = if (isPaired) p[Keys.TV_NAME].orEmpty() else "",
                    sessionToken = effectiveToken,
                    deviceId = if (isPaired) p[Keys.TV_DEVICE_ID].orEmpty() else "",
                    pairedAt = if (isPaired) (p[Keys.TV_PAIRED_AT] ?: 0L) else 0L
                )
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

    override suspend fun savePairedTv(info: PairedTvInfo): Result<Unit> {
        val encryptedResult = if (info.sessionToken.isNotBlank()) {
            MobileTokenCipher.encryptToken(info.sessionToken)
        } else {
            Result.success("")
        }

        if (encryptedResult.isFailure) {
            return Result.failure(encryptedResult.exceptionOrNull() ?: IllegalStateException("Failed to securely encrypt pairing token"))
        }

        val encryptedToken = encryptedResult.getOrThrow()
        context.mobileStore.edit { p ->
            p[Keys.TV_HOST] = info.host
            p[Keys.TV_PORT] = info.port
            p[Keys.TV_NAME] = info.tvDeviceName
            p[Keys.TV_SESSION_TOKEN] = encryptedToken
            p[Keys.TV_DEVICE_ID] = info.deviceId
            p[Keys.TV_PAIRED_AT] = info.pairedAt
        }
        return Result.success(Unit)
    }

    override suspend fun clearPairedTv() {
        context.mobileStore.edit { p ->
            p.remove(Keys.TV_HOST)
            p.remove(Keys.TV_PORT)
            p.remove(Keys.TV_NAME)
            p.remove(Keys.TV_SESSION_TOKEN)
            p.remove(Keys.TV_DEVICE_ID)
            p.remove(Keys.TV_PAIRED_AT)
        }
    }
}
