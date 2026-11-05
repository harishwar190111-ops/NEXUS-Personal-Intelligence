package com.nexus.personalintelligence.tv.network

import android.content.Context
import android.content.ContextWrapper
import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import com.nexus.personalintelligence.tv.data.AppRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class TvCommandDispatcherTest {

    private class TestContext : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun getPackageName(): String = "com.nexus.personalintelligence.tv"
    }

    private lateinit var dummyContext: Context

    @Before
    fun setUp() {
        dummyContext = TestContext()
    }

    @Test
    fun testDpadWithoutTargetFailsWithExplicitMessage() = runBlocking {
        val dispatcher = TvCommandDispatcher(context = dummyContext)
        val result = dispatcher.dispatch(RemoteCommand(type = RemoteCommandType.DPAD_UP))

        assertFalse("DPAD command without target must not succeed blindly", result.success)
        assertTrue(result.message.contains("unavailable"))
    }

    @Test
    fun testDpadWithDirectionHandlerSucceeds() = runBlocking {
        val receivedType = AtomicReference<RemoteCommandType>()
        val dispatcher = TvCommandDispatcher(
            context = dummyContext,
            onNavigateDirection = {
                receivedType.set(it)
                true
            }
        )
        val result = dispatcher.dispatch(RemoteCommand(type = RemoteCommandType.DPAD_CENTER))

        assertTrue(result.success)
        assertEquals(RemoteCommandType.DPAD_CENTER, receivedType.get())
    }

    @Test
    fun testPowerReturnsExplicitUnavailableFailure() = runBlocking {
        val dispatcher = TvCommandDispatcher(context = dummyContext)
        val result = dispatcher.dispatch(RemoteCommand(type = RemoteCommandType.POWER))

        assertFalse("POWER command must not return success without device administration permission", result.success)
        assertTrue(result.message.contains("unavailable"))
    }

    @Test
    fun testTextInputDeliveredWithoutExposingTextInMessage() = runBlocking {
        val receivedText = AtomicReference<String>()
        val dispatcher = TvCommandDispatcher(
            context = dummyContext,
            onTextReceived = {
                receivedText.set(it)
                true
            }
        )

        val secretInput = "MY_SECRET_PASSWORD_OR_QUERY"
        val result = dispatcher.dispatch(
            RemoteCommand(
                type = RemoteCommandType.TEXT_INPUT,
                text = secretInput
            )
        )

        assertTrue(result.success)
        assertEquals("Text input delivered", result.message)
        assertEquals(secretInput, receivedText.get())

        // Critical requirement: Never leak typed text into result message, history or events
        assertFalse(result.message.contains(secretInput))
        for (entry in dispatcher.historyTracker.getHistory()) {
            assertFalse("History must not contain typed text", entry.message.contains(secretInput))
        }
        for (event in dispatcher.eventStore.listEvents()) {
            assertFalse("Event store must not contain typed text", event.message.contains(secretInput))
        }
    }

    @Test
    fun testTextInputWithoutReceiverFails() = runBlocking {
        val dispatcher = TvCommandDispatcher(context = dummyContext, onTextReceived = null)
        val result = dispatcher.dispatch(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = "Hello"))

        assertFalse("Text input without receiver must fail", result.success)
        assertTrue(result.message.contains("No active text receiver"))
    }

    @Test
    fun testPointerWithoutReceiverFails() = runBlocking {
        val dispatcher = TvCommandDispatcher(context = dummyContext)
        val result = dispatcher.dispatch(RemoteCommand(type = RemoteCommandType.POINTER_MOVE, deltaX = 10f, deltaY = 20f))

        assertFalse("Pointer move without active touch surface must fail", result.success)
        assertTrue(result.message.contains("unavailable"))
    }

    @Test
    fun testHomeNavigatesWithHandler() = runBlocking {
        val homeCalled = AtomicBoolean(false)
        val dispatcher = TvCommandDispatcher(
            context = dummyContext,
            onNavigateHome = {
                homeCalled.set(true)
                true
            }
        )
        val result = dispatcher.dispatch(RemoteCommand(type = RemoteCommandType.HOME))

        assertTrue(result.success)
        assertTrue(homeCalled.get())
        assertEquals("Navigated to HOME", result.message)
    }

    @Test
    fun testRealDispatcherTextInputBackspaceAndEnterUpdatesFieldState() = runBlocking {
        var enterSubmitted = false
        val fieldState = com.nexus.personalintelligence.tv.ui.TvEditableFieldState(
            initialText = "",
            onEnter = { enterSubmitted = true }
        )
        val coordinator = com.nexus.personalintelligence.tv.TvTextInputCoordinator()
        coordinator.bindFieldState(fieldState)

        val dispatcher = TvCommandDispatcher(
            context = dummyContext,
            onTextReceived = { text -> coordinator.handleReceivedText(text) }
        )

        // 1. Send TEXT_INPUT
        val textResult = dispatcher.dispatch(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = "NexusSearch"))
        assertTrue(textResult.success)
        assertEquals("NexusSearch", fieldState.text)
        assertEquals(11, fieldState.cursorPosition)
        assertEquals("Text input delivered", textResult.message)

        // 2. Send BACKSPACE
        val bsResult = dispatcher.dispatch(RemoteCommand(type = RemoteCommandType.BACKSPACE))
        assertTrue(bsResult.success)
        assertEquals("NexusSearc", fieldState.text)
        assertEquals(10, fieldState.cursorPosition)
        assertEquals("Backspace delivered", bsResult.message)

        // 3. Send ENTER
        val enterResult = dispatcher.dispatch(RemoteCommand(type = RemoteCommandType.ENTER))
        assertTrue(enterResult.success)
        assertTrue("ENTER must invoke onEnter action on fieldState", enterSubmitted)
        assertEquals("Enter delivered", enterResult.message)

        // 4. Test masking choice on field state
        assertFalse(fieldState.isMasked)
        assertEquals("NexusSearc", fieldState.getDisplayText())
        fieldState.toggleMasked()
        assertTrue(fieldState.isMasked)
        assertEquals("••••••••••", fieldState.getDisplayText())

        // 5. When unbind is called, dispatcher text commands fail honestly
        coordinator.unbindVisibleInputField()
        val failResult = dispatcher.dispatch(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = "More"))
        assertFalse("Without active text receiver, TEXT_INPUT must fail", failResult.success)
        assertEquals("No active text receiver available on TV", failResult.message)
    }
}
