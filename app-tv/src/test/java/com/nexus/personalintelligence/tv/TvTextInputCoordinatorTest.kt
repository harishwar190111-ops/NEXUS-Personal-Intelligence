package com.nexus.personalintelligence.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class TvTextInputCoordinatorTest {

    @Test
    fun testHandleReceivedTextReturnsFalseWhenReceiverNull() {
        val coordinator = TvTextInputCoordinator()
        assertFalse("Must return false when no active receiver is attached", coordinator.handleReceivedText("Hello"))
        assertEquals("Buffer must remain empty without active receiver", "", coordinator.receivedTextBuffer.value)
    }

    @Test
    fun testHandleReceivedTextCollectsOnlyWhenWiredReceiverAccepts() {
        val coordinator = TvTextInputCoordinator()
        val captured = AtomicReference<String>("")

        // Wire visible input field
        coordinator.bindVisibleInputField { text ->
            captured.set(text)
            true
        }

        val success = coordinator.handleReceivedText("NEXUS")
        assertTrue("Must return true when receiver accepts", success)
        assertEquals("NEXUS", captured.get())
        assertEquals("NEXUS", coordinator.receivedTextBuffer.value)

        // Backspace
        val backspaceSuccess = coordinator.handleReceivedText("\b")
        assertTrue(backspaceSuccess)
        assertEquals("NEXU", coordinator.receivedTextBuffer.value)
    }

    @Test
    fun testHandleReceivedTextFailsWhenReceiverRejects() {
        val coordinator = TvTextInputCoordinator()

        // Wire receiver that rejects invalid characters
        coordinator.bindVisibleInputField { text ->
            text.all { it.isDigit() }
        }

        val rejected = coordinator.handleReceivedText("letters-not-allowed")
        assertFalse("Must return false when visible input field rejects input", rejected)
        assertEquals("Buffer must not collect rejected input", "", coordinator.receivedTextBuffer.value)

        val accepted = coordinator.handleReceivedText("12345")
        assertTrue(accepted)
        assertEquals("12345", coordinator.receivedTextBuffer.value)
    }

    @Test
    fun testUnbindClearsBufferAndRejectsSubsequentInputs() {
        val coordinator = TvTextInputCoordinator()
        coordinator.bindVisibleInputField()

        coordinator.handleReceivedText("test")
        assertEquals("test", coordinator.receivedTextBuffer.value)

        coordinator.unbindVisibleInputField()
        assertEquals("Buffer must be cleared on unbind", "", coordinator.receivedTextBuffer.value)

        assertFalse("Subsequent text input must return false after unbind", coordinator.handleReceivedText("more"))
    }
}
