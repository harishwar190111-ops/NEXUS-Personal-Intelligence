package com.nexus.personalintelligence.core.memory

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class MemoryCategory { FAVOURITE_APP, PREFERRED_THEME, AUDIO_PREFERENCE, REMOTE_LAYOUT, USER_CUSTOM_NOTE }

@Serializable
data class NexusMemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val category: MemoryCategory,
    val timestamp: Long = System.currentTimeMillis()
)

class NexusMemoryRepository {
    private val _memories = MutableStateFlow<Map<String, NexusMemoryItem>>(emptyMap())
    val memories: StateFlow<Map<String, NexusMemoryItem>> = _memories.asStateFlow()
    private var isMemoryEnabled = true

    fun setMemoryEnabled(enabled: Boolean) {
        isMemoryEnabled = enabled
        if (!enabled) clearAll()
    }
    fun isEnabled(): Boolean = isMemoryEnabled

    @Synchronized
    fun addOrUpdate(key: String, value: String, category: MemoryCategory): Boolean {
        if (!isMemoryEnabled) return false
        if (key.contains("key", ignoreCase = true) || key.contains("secret", ignoreCase = true)) return false
        val item = NexusMemoryItem(key = key, value = value, category = category)
        val cur = _memories.value.toMutableMap()
        cur[key] = item
        _memories.value = cur
        return true
    }

    @Synchronized fun delete(key: String) {
        val cur = _memories.value.toMutableMap()
        cur.remove(key)
        _memories.value = cur
    }

    @Synchronized fun clearAll() { _memories.value = emptyMap() }

    fun exportToJson(): String = Json.encodeToString(_memories.value.values.toList())
    fun importFromJson(jsonString: String): Boolean = try {
        val list = Json.decodeFromString<List<NexusMemoryItem>>(jsonString)
        _memories.value = list.associateBy { it.key }
        true
    } catch (_: Exception) { false }
}
