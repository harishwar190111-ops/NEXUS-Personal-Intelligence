package com.nexus.personalintelligence.core.storage

import com.nexus.personalintelligence.core.design.BackgroundConfig
import com.nexus.personalintelligence.core.design.NexusTheme
import com.nexus.personalintelligence.core.design.NexusThemePreset
import com.nexus.personalintelligence.core.design.ThemeRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class CustomTileType { APP, WEBSITE, NEXUS_ACTION }

@Serializable
data class CustomTile(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: CustomTileType,
    val target: String,
    val category: String = "Default",
    val isFavorite: Boolean = false,
    val order: Int = 0
)

@Serializable
data class NexusFullBackup(
    val theme: NexusTheme,
    val background: BackgroundConfig,
    val customTiles: List<CustomTile>,
    val soundEnabled: Boolean = true,
    val uiVolume: Float = 0.8f,
    val quietMode: Boolean = false,
    val overscanPaddingDp: Int = 16,
    val timestamp: Long = System.currentTimeMillis()
)

class NexusSettingsRepository {
    private val _activeTheme = MutableStateFlow(ThemeRegistry.getTheme(NexusThemePreset.LIQUID_GLASS))
    val activeTheme: StateFlow<NexusTheme> = _activeTheme.asStateFlow()

    private val _backgroundConfig = MutableStateFlow(BackgroundConfig())
    val backgroundConfig: StateFlow<BackgroundConfig> = _backgroundConfig.asStateFlow()

    private val _customTiles = MutableStateFlow<List<CustomTile>>(emptyList())
    val customTiles: StateFlow<List<CustomTile>> = _customTiles.asStateFlow()

    fun setTheme(theme: NexusTheme) { _activeTheme.value = theme }
    fun setBackgroundConfig(config: BackgroundConfig) { _backgroundConfig.value = config }

    fun addOrUpdateTile(tile: CustomTile) {
        val cur = _customTiles.value.toMutableList()
        val idx = cur.indexOfFirst { it.id == tile.id }
        if (idx != -1) cur[idx] = tile else cur.add(tile)
        _customTiles.value = cur
    }

    fun deleteTile(id: String) {
        _customTiles.value = _customTiles.value.filter { it.id != id }
    }

    fun exportBackup(): String {
        return Json.encodeToString(
            NexusFullBackup(
                theme = _activeTheme.value,
                background = _backgroundConfig.value,
                customTiles = _customTiles.value
            )
        )
    }

    fun restoreBackup(backupJson: String): Boolean {
        return try {
            val b = Json.decodeFromString<NexusFullBackup>(backupJson)
            _activeTheme.value = b.theme
            _backgroundConfig.value = b.background
            _customTiles.value = b.customTiles
            true
        } catch (_: Exception) { false }
    }
}
