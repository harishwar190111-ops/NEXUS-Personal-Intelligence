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
    private val appProvider: (() -> List<TvInstalledApp>)? = null,
    private val stateProvider: (() -> TvManagementResponse)? = null,
    private val onThemeChanged: ((String) -> Unit)? = null,
    private val onWallpaperChanged: ((String) -> Unit)? = null,
    private val onFavouritesUpdated: ((List<String>) -> Unit)? = null,
    private val onAppVisibilityChanged: ((packageName: String, isHidden: Boolean) -> Unit)? = null,
    private val onClockStyleChanged: ((Boolean) -> Unit)? = null,
    private val onHomeLayoutChanged: ((String) -> Unit)? = null,
    private val storageMetricsProvider: (() -> Pair<Long, Long>)? = null,
    private val themeValidator: ((String) -> Boolean)? = null,
    private val wallpaperValidator: ((String) -> Boolean)? = null
) {
    fun handleRequest(request: TvManagementRequest): TvManagementResponse {
        return when (request.action) {
            TvManagementAction.GET_INSTALLED_APPS -> {
                val provider = appProvider
                if (provider == null) {
                    TvManagementResponse(request.requestId, request.action, false, "GET_INSTALLED_APPS unsupported: no application repository wired")
                } else {
                    TvManagementResponse(
                        requestId = request.requestId,
                        action = request.action,
                        success = true,
                        installedApps = provider()
                    )
                }
            }
            TvManagementAction.GET_TV_STATE -> {
                val provider = stateProvider
                if (provider == null) {
                    TvManagementResponse(request.requestId, request.action, false, "GET_TV_STATE unsupported: no state provider wired")
                } else {
                    val state = provider()
                    state.copy(requestId = request.requestId, action = request.action, success = true)
                }
            }
            TvManagementAction.SET_THEME_PRESET -> {
                val handler = onThemeChanged
                if (handler == null) {
                    TvManagementResponse(request.requestId, request.action, false, "SET_THEME_PRESET unsupported: no theme preference handler wired")
                } else {
                    val themeId = request.payload.trim()
                    if (themeId.isBlank()) {
                        TvManagementResponse(request.requestId, request.action, false, "Theme preset ID cannot be blank")
                    } else if (themeValidator != null && !themeValidator(themeId)) {
                        TvManagementResponse(request.requestId, request.action, false, "Unknown theme preset ID: $themeId")
                    } else {
                        handler(themeId)
                        TvManagementResponse(request.requestId, request.action, true, "Theme set to $themeId", themePresetId = themeId)
                    }
                }
            }
            TvManagementAction.SET_WALLPAPER -> {
                val handler = onWallpaperChanged
                if (handler == null) {
                    TvManagementResponse(request.requestId, request.action, false, "SET_WALLPAPER unsupported: no wallpaper preference handler wired")
                } else {
                    val path = request.payload.trim()
                    if (path.isBlank()) {
                        TvManagementResponse(request.requestId, request.action, false, "Wallpaper path cannot be blank")
                    } else if (wallpaperValidator != null && !wallpaperValidator(path)) {
                        TvManagementResponse(request.requestId, request.action, false, "Invalid wallpaper path or inaccessible image: $path")
                    } else {
                        handler(path)
                        TvManagementResponse(request.requestId, request.action, true, "Wallpaper updated", wallpaperPath = path)
                    }
                }
            }
            TvManagementAction.SET_FAVOURITES -> {
                val handler = onFavouritesUpdated
                if (handler == null) {
                    TvManagementResponse(request.requestId, request.action, false, "SET_FAVOURITES unsupported: no favourites preference handler wired")
                } else {
                    val pkgs = request.payload.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    handler(pkgs)
                    TvManagementResponse(request.requestId, request.action, true, "Favourites updated", favourites = pkgs)
                }
            }
            TvManagementAction.SET_APP_VISIBILITY -> {
                val handler = onAppVisibilityChanged
                if (handler == null) {
                    TvManagementResponse(request.requestId, request.action, false, "SET_APP_VISIBILITY unsupported: no app visibility handler wired")
                } else {
                    val parts = request.payload.split(":")
                    val pkg = parts.getOrNull(0)?.trim()
                    val hide = parts.getOrNull(1)?.trim() == "hide"
                    if (pkg.isNullOrBlank()) {
                        TvManagementResponse(request.requestId, request.action, false, "Package name required")
                    } else {
                        handler(pkg, hide)
                        TvManagementResponse(request.requestId, request.action, true, "App $pkg visibility updated to ${if (hide) "hidden" else "visible"}")
                    }
                }
            }
            TvManagementAction.SET_CLOCK_STYLE -> {
                val handler = onClockStyleChanged
                if (handler == null) {
                    TvManagementResponse(request.requestId, request.action, false, "SET_CLOCK_STYLE unsupported: no clock style preference handler wired")
                } else {
                    val use24 = request.payload.trim().let { it == "24" || it.equals("true", ignoreCase = true) }
                    handler(use24)
                    TvManagementResponse(request.requestId, request.action, true, "Clock style updated to ${if (use24) "24-hour" else "12-hour"}", clockStyle24Hour = use24)
                }
            }
            TvManagementAction.SET_HOME_LAYOUT -> {
                val handler = onHomeLayoutChanged
                if (handler == null) {
                    TvManagementResponse(request.requestId, request.action, false, "SET_HOME_LAYOUT unsupported: no layout preference handler wired")
                } else {
                    val layout = request.payload.trim().ifBlank { "ROWS" }
                    handler(layout)
                    TvManagementResponse(request.requestId, request.action, true, "Home layout updated to $layout", homeLayout = layout)
                }
            }
            TvManagementAction.GET_STORAGE_METRICS -> {
                val provider = storageMetricsProvider
                if (provider == null) {
                    TvManagementResponse(request.requestId, request.action, false, "GET_STORAGE_METRICS unsupported: no storage metric provider wired")
                } else {
                    val (free, total) = provider()
                    TvManagementResponse(
                        requestId = request.requestId,
                        action = request.action,
                        success = true,
                        message = "Storage metrics retrieved",
                        freeStorageBytes = free,
                        totalStorageBytes = total
                    )
                }
            }
            else -> {
                TvManagementResponse(request.requestId, request.action, false, "Unsupported management action: ${request.action}")
            }
        }
    }
}
