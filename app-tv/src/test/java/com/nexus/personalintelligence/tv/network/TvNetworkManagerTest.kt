package com.nexus.personalintelligence.tv.network

import android.view.KeyEvent
import com.nexus.personalintelligence.core.remote.CommandResult
import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class TvNetworkManagerTest {

    private var networkManager: TvNetworkManager? = null

    @Before
    fun setUp() {
        networkManager = TvNetworkManager(port = 0, tvDeviceName = "Test TV Stick")
        networkManager?.start()
    }

    @After
    fun tearDown() {
        networkManager?.stop()
    }

    @Test
    fun testServerLifecycle() {
        val mgr = networkManager ?: throw IllegalStateException("mgr is null")
        assertTrue("Server should be running after start", mgr.isServerRunning.value)
        mgr.stop()
        assertFalse("Server should not be running after stop", mgr.isServerRunning.value)
    }

    @Test
    fun testCommandHandlerInvocation() = runBlocking {
        val mgr = networkManager ?: throw IllegalStateException("mgr is null")
        val called = AtomicBoolean(false)

        mgr.commandHandler = { cmd ->
            called.set(true)
            assertEquals(RemoteCommandType.DPAD_UP, cmd.type)
            CommandResult(commandId = cmd.commandId, success = true, message = "Handled DPAD_UP")
        }

        val result = mgr.commandHandler?.invoke(RemoteCommand(type = RemoteCommandType.DPAD_UP))
        assertNotNull(result)
        assertTrue(called.get())
        assertTrue(result?.success == true)
        assertEquals("Handled DPAD_UP", result?.message)
    }

    @Test
    fun testKeyCodeMapping() {
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, TvCommandDispatcher.toKeyCode(RemoteCommandType.DPAD_UP))
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, TvCommandDispatcher.toKeyCode(RemoteCommandType.DPAD_DOWN))
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, TvCommandDispatcher.toKeyCode(RemoteCommandType.DPAD_LEFT))
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, TvCommandDispatcher.toKeyCode(RemoteCommandType.DPAD_RIGHT))
        assertEquals(KeyEvent.KEYCODE_DPAD_CENTER, TvCommandDispatcher.toKeyCode(RemoteCommandType.DPAD_CENTER))
        assertEquals(KeyEvent.KEYCODE_BACK, TvCommandDispatcher.toKeyCode(RemoteCommandType.BACK))
        assertEquals(KeyEvent.KEYCODE_HOME, TvCommandDispatcher.toKeyCode(RemoteCommandType.HOME))
        assertEquals(KeyEvent.KEYCODE_MENU, TvCommandDispatcher.toKeyCode(RemoteCommandType.MENU))
        assertEquals(KeyEvent.KEYCODE_SETTINGS, TvCommandDispatcher.toKeyCode(RemoteCommandType.SETTINGS))
        assertEquals(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, TvCommandDispatcher.toKeyCode(RemoteCommandType.PLAY_PAUSE))
        assertEquals(KeyEvent.KEYCODE_VOLUME_UP, TvCommandDispatcher.toKeyCode(RemoteCommandType.VOLUME_UP))
        assertEquals(KeyEvent.KEYCODE_VOLUME_DOWN, TvCommandDispatcher.toKeyCode(RemoteCommandType.VOLUME_DOWN))
        assertEquals(KeyEvent.KEYCODE_VOLUME_MUTE, TvCommandDispatcher.toKeyCode(RemoteCommandType.MUTE))
    }
}
