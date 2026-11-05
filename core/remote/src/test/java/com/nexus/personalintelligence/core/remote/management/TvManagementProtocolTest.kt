package com.nexus.personalintelligence.core.remote.management

import com.nexus.personalintelligence.core.remote.ConnectionState
import com.nexus.personalintelligence.core.remote.NexusSocketClient
import com.nexus.personalintelligence.core.remote.NexusSocketServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class TvManagementProtocolTest {

    private var server: NexusSocketServer? = null
    private var client: NexusSocketClient? = null
    private var serverPort: Int = 0

    private val mockInstalledApps = listOf(
        TvInstalledApp(packageName = "com.google.android.youtube.tv", label = "YouTube TV", isFavourite = true),
        TvInstalledApp(packageName = "com.netflix.ninja", label = "Netflix", isFavourite = false),
        TvInstalledApp(packageName = "com.android.tv.settings", label = "Settings", isSystemApp = true)
    )

    private val updatedTheme = AtomicReference<String>("")
    private val updatedWallpaper = AtomicReference<String>("")
    private val updatedFavourites = AtomicReference<List<String>>(emptyList())

    @Before
    fun setUp() {
        val managementHandler = TvManagementProtocolHandler(
            appProvider = { mockInstalledApps },
            stateProvider = {
                TvManagementResponse(
                    action = TvManagementAction.GET_TV_STATE,
                    success = true,
                    themePresetId = "obsidian",
                    wallpaperPath = "/data/wallpaper.png",
                    clockStyle24Hour = true,
                    homeLayout = "ROWS",
                    favourites = listOf("com.google.android.youtube.tv")
                )
            },
            onThemeChanged = { updatedTheme.set(it) },
            onWallpaperChanged = { updatedWallpaper.set(it) },
            onFavouritesUpdated = { updatedFavourites.set(it) }
        )

        server = NexusSocketServer(
            port = 0,
            tvDeviceName = "Living Room TV",
            tvManagementHandler = managementHandler
        )
        server?.start()
        serverPort = server?.actualPort ?: throw IllegalStateException("Port not bound")

        client = NexusSocketClient()
    }

    @After
    fun tearDown() {
        client?.disconnect()
        server?.stop()
    }

    private suspend fun pairAndAuthenticateClient(): Pair<String, String> {
        val s = server ?: throw IllegalStateException("Server null")
        val c = client ?: throw IllegalStateException("Client null")

        val clientDeviceId = "phone-dev-123"
        val clientDeviceName = "Samsung Galaxy F54"

        val gateCalled = AtomicBoolean(false)
        var serverPin = ""
        s.securityGateCallback = { _, _, pin, onApprove, _ ->
            gateCalled.set(true)
            serverPin = pin
            onApprove()
        }

        c.connect("127.0.0.1", serverPort)
        c.requestPairing(clientDeviceId, clientDeviceName)
        val verifyResp = c.verifyPairing(clientDeviceId, serverPin)
        val token = verifyResp.sessionToken ?: throw IllegalStateException("Token null")

        c.authenticate(clientDeviceId, token, clientDeviceName)
        assertEquals(ConnectionState.CONNECTED, c.connectionState.value)
        return Pair(clientDeviceId, token)
    }

    @Test
    fun testGetInstalledAppsFromMobile() = runBlocking {
        pairAndAuthenticateClient()
        val c = client ?: throw IllegalStateException("Client null")

        val req = TvManagementRequest(action = TvManagementAction.GET_INSTALLED_APPS)
        val resp = c.sendTvManagementRequest(req)

        assertTrue(resp.success)
        assertEquals(TvManagementAction.GET_INSTALLED_APPS, resp.action)
        assertEquals(3, resp.installedApps.size)
        assertEquals("YouTube TV", resp.installedApps[0].label)
        assertTrue(resp.installedApps[0].isFavourite)
        assertEquals("com.netflix.ninja", resp.installedApps[1].packageName)
    }

    @Test
    fun testSetThemePresetFromMobile() = runBlocking {
        pairAndAuthenticateClient()
        val c = client ?: throw IllegalStateException("Client null")

        val req = TvManagementRequest(
            action = TvManagementAction.SET_THEME_PRESET,
            payload = "neon_matrix"
        )
        val resp = c.sendTvManagementRequest(req)

        assertTrue(resp.success)
        assertEquals("neon_matrix", updatedTheme.get())
        assertEquals("neon_matrix", resp.themePresetId)
    }

    @Test
    fun testSetWallpaperFromMobile() = runBlocking {
        pairAndAuthenticateClient()
        val c = client ?: throw IllegalStateException("Client null")

        val req = TvManagementRequest(
            action = TvManagementAction.SET_WALLPAPER,
            payload = "/storage/emulated/0/wallpapers/cyber.jpg"
        )
        val resp = c.sendTvManagementRequest(req)

        assertTrue(resp.success)
        assertEquals("/storage/emulated/0/wallpapers/cyber.jpg", updatedWallpaper.get())
        assertEquals("/storage/emulated/0/wallpapers/cyber.jpg", resp.wallpaperPath)
    }

    @Test
    fun testSetFavouritesFromMobile() = runBlocking {
        pairAndAuthenticateClient()
        val c = client ?: throw IllegalStateException("Client null")

        val req = TvManagementRequest(
            action = TvManagementAction.SET_FAVOURITES,
            payload = "com.netflix.ninja, com.google.android.youtube.tv"
        )
        val resp = c.sendTvManagementRequest(req)

        assertTrue(resp.success)
        val favs = updatedFavourites.get()
        assertEquals(2, favs.size)
        assertEquals("com.netflix.ninja", favs[0])
        assertEquals("com.google.android.youtube.tv", favs[1])
    }

    @Test
    fun testRejectUnauthorizedManagementRequest() = runBlocking {
        val s = server ?: throw IllegalStateException("Server null")
        val c = client ?: throw IllegalStateException("Client null")

        c.connect("127.0.0.1", serverPort)
        // Send request with bogus token without authenticating
        val req = TvManagementRequest(action = TvManagementAction.GET_INSTALLED_APPS)
        val resp = c.sendTvManagementRequest(req, sessionToken = "invalid-bogus-token")

        assertFalse("Unauthorized request must be rejected", resp.success)
        assertTrue(resp.message.contains("Unauthorized") || resp.message.contains("rejected"))
    }
}
