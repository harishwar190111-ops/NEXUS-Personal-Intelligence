package com.nexus.personalintelligence.tv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexus.personalintelligence.core.common.ui.BackgroundCodec
import com.nexus.personalintelligence.core.common.ui.BackgroundSpec
import com.nexus.personalintelligence.core.common.ui.CustomTheme
import com.nexus.personalintelligence.core.common.ui.NexusAccent
import com.nexus.personalintelligence.core.common.ui.NexusBackdrop
import com.nexus.personalintelligence.core.common.ui.ResolvedTheme
import com.nexus.personalintelligence.core.common.ui.ThemePresets
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class TvPrefs(
    val favourites: List<String> = emptyList(),
    val recents: List<String> = emptyList(),
    val presetId: String = ThemePresets.DEFAULT_ID,
    val custom: CustomTheme = CustomTheme(),
    val backgroundOverride: BackgroundSpec? = null,
    val imagePath: String? = null,
    val use24Hour: Boolean = true,
    val webApps: List<WebShortcut> = emptyList(),
) {
    val theme: ResolvedTheme get() = ThemePresets.resolve(presetId, custom, backgroundOverride)
}

private val Context.tvDataStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_tv_prefs")

class TvPrefsRepository(private val context: Context) {
    private object Keys {
        val FAVOURITES = stringPreferencesKey("favourites")
        val RECENTS = stringPreferencesKey("recents")
        val PRESET = stringPreferencesKey("preset")
        val CUSTOM_ACCENT = stringPreferencesKey("custom_accent")
        val CUSTOM_BACKDROP = stringPreferencesKey("custom_backdrop")
        val CUSTOM_CORNER = intPreferencesKey("custom_corner")
        val CUSTOM_GLASS = intPreferencesKey("custom_glass")
        val BG_OVERRIDE = stringPreferencesKey("bg_override")
        val BG_IMAGE = stringPreferencesKey("bg_image")
        val CLOCK_24H = booleanPreferencesKey("clock_24h")
        val WEB_APPS = stringPreferencesKey("web_apps")
    }

    val prefs: Flow<TvPrefs> = context.tvDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val defaults = CustomTheme()
            TvPrefs(
                favourites = PrefsLogic.decode(p[Keys.FAVOURITES]),
                recents = PrefsLogic.decode(p[Keys.RECENTS]),
                presetId = ThemePresets.byId(p[Keys.PRESET]).id,
                custom = CustomTheme(
                    accent = NexusAccent.fromId(p[Keys.CUSTOM_ACCENT]),
                    backdrop = NexusBackdrop.fromId(p[Keys.CUSTOM_BACKDROP]),
                    cornerDp = (p[Keys.CUSTOM_CORNER] ?: defaults.cornerDp).coerceIn(0, 48),
                    glassPercent = (p[Keys.CUSTOM_GLASS] ?: defaults.glassPercent).coerceIn(2, 30),
                ),
                backgroundOverride = BackgroundCodec.decode(p[Keys.BG_OVERRIDE]),
                imagePath = p[Keys.BG_IMAGE],
                use24Hour = p[Keys.CLOCK_24H] ?: true,
                webApps = WebLogic.decode(p[Keys.WEB_APPS]),
            )
        }

    suspend fun toggleFavourite(pkg: String) {
        context.tvDataStore.edit { p ->
            p[Keys.FAVOURITES] = PrefsLogic.encode(PrefsLogic.toggled(PrefsLogic.decode(p[Keys.FAVOURITES]), pkg))
        }
    }

    suspend fun pushRecent(pkg: String) {
        context.tvDataStore.edit { p ->
            p[Keys.RECENTS] = PrefsLogic.encode(PrefsLogic.withRecent(PrefsLogic.decode(p[Keys.RECENTS]), pkg))
        }
    }

    suspend fun clearRecents() {
        context.tvDataStore.edit { p -> p.remove(Keys.RECENTS) }
    }

    suspend fun clearFavourites() {
        context.tvDataStore.edit { p -> p.remove(Keys.FAVOURITES) }
    }

    suspend fun setFavourites(favourites: List<String>) {
        context.tvDataStore.edit { p ->
            p[Keys.FAVOURITES] = PrefsLogic.encode(favourites)
        }
    }

    suspend fun setPreset(id: String) {
        context.tvDataStore.edit { p -> p[Keys.PRESET] = id }
    }

    suspend fun setCustom(custom: CustomTheme) {
        context.tvDataStore.edit { p ->
            p[Keys.CUSTOM_ACCENT] = custom.accent.name
            p[Keys.CUSTOM_BACKDROP] = custom.backdrop.name
            p[Keys.CUSTOM_CORNER] = custom.cornerDp
            p[Keys.CUSTOM_GLASS] = custom.glassPercent
        }
    }

    suspend fun setBackground(spec: BackgroundSpec) {
        context.tvDataStore.edit { p -> p[Keys.BG_OVERRIDE] = BackgroundCodec.encode(spec) }
    }

    suspend fun resetBackground() {
        context.tvDataStore.edit { p -> p.remove(Keys.BG_OVERRIDE) }
    }

    suspend fun setImagePath(path: String?) {
        context.tvDataStore.edit { p -> if (path == null) p.remove(Keys.BG_IMAGE) else p[Keys.BG_IMAGE] = path }
    }

    suspend fun setUse24Hour(value: Boolean) {
        context.tvDataStore.edit { p -> p[Keys.CLOCK_24H] = value }
    }

    suspend fun editWebApps(transform: (List<WebShortcut>) -> List<WebShortcut>) {
        context.tvDataStore.edit { p ->
            p[Keys.WEB_APPS] = WebLogic.encode(transform(WebLogic.decode(p[Keys.WEB_APPS])))
        }
    }
}
