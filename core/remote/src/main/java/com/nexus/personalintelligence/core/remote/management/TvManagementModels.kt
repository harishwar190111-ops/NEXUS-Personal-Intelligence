package com.nexus.personalintelligence.core.remote.management

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TvManagementAction {
    GET_INSTALLED_APPS,
    GET_TV_STATE,
    SET_THEME_PRESET,
    SET_WALLPAPER,
    SET_CLOCK_STYLE,
    SET_HOME_LAYOUT,
    SET_FAVOURITES,
    SET_APP_VISIBILITY,
    GET_STORAGE_METRICS
}

@Serializable
data class TvInstalledApp(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean = false,
    val isHidden: Boolean = false,
    val isFavourite: Boolean = false
)

@Serializable
data class TvManagementRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val action: TvManagementAction,
    val payload: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TvManagementResponse(
    val requestId: String = UUID.randomUUID().toString(),
    val action: TvManagementAction,
    val success: Boolean,
    val message: String = "",
    val installedApps: List<TvInstalledApp> = emptyList(),
    val themePresetId: String = "",
    val wallpaperPath: String = "",
    val clockStyle24Hour: Boolean = true,
    val homeLayout: String = "ROWS",
    val favourites: List<String> = emptyList(),
    val freeStorageBytes: Long = -1L,
    val totalStorageBytes: Long = -1L
)

open class TvManagementProtocolHandler(
    private val appProvider: () -> List<TvInstalledApp> = { emptyList() },
    private val stateProvider: () -> TvManagementResponse = {
        TvManagementResponse(
            action = TvManagementAction.GET_TV_STATE,
            success = true
        )
    },
    private val onThemeChanged: (String) -> Unit = {},
    private val onWallpaperChanged: (String) -> Unit = {},
    private val onFavouritesUpdated: (List<String>) -> Unit = {},
    private val onAppVisibilityChanged: (packageName: String, isHidden: Boolean) -> Unit = { _, _ -> }
) {
    fun handleRequest(request: TvManagementRequest): TvManagementResponse {
        return when (request.action) {
            TvManagementAction.GET_INSTALLED_APPS -> {
                TvManagementResponse(
                    requestId = request.requestId,
                    action = request.action,
                    success = true,
                    installedApps = appProvider()
                )
            }
            TvManagementAction.GET_TV_STATE -> {
                val state = stateProvider()
                state.copy(requestId = request.requestId, action = request.action, success = true)
            }
            TvManagementAction.SET_THEME_PRESET -> {
                val themeId = request.payload.trim()
                if (themeId.isBlank()) {
                    TvManagementResponse(request.requestId, request.action, false, "Theme preset ID cannot be blank")
                } else {
                    onThemeChanged(themeId)
                    TvManagementResponse(request.requestId, request.action, true, "Theme set to $themeId", themePresetId = themeId)
                }
            }
            TvManagementAction.SET_WALLPAPER -> {
                val path = request.payload.trim()
                onWallpaperChanged(path)
                TvManagementResponse(request.requestId, request.action, true, "Wallpaper updated", wallpaperPath = path)
            }
            TvManagementAction.SET_FAVOURITES -> {
                val pkgs = request.payload.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onFavouritesUpdated(pkgs)
                TvManagementResponse(request.requestId, request.action, true, "Favourites updated", favourites = pkgs)
            }
            TvManagementAction.SET_APP_VISIBILITY -> {
                val parts = request.payload.split(":")
                val pkg = parts.getOrNull(0)?.trim()
                val hide = parts.getOrNull(1)?.trim() == "hide"
                if (pkg.isNullOrBlank()) {
                    TvManagementResponse(request.requestId, request.action, false, "Package name required")
                } else {
                    onAppVisibilityChanged(pkg, hide)
                    TvManagementResponse(request.requestId, request.action, true, "App $pkg visibility updated to ${if (hide) "hidden" else "visible"}")
                }
            }
            else -> {
                TvManagementResponse(request.requestId, request.action, true, "Action acknowledged")
            }
        }
    }
}
