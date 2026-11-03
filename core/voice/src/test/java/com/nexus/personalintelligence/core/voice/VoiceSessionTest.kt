package com.nexus.personalintelligence.core.voice

import org.junit.Assert.*
import org.junit.Test

class NexusVoiceSessionManagerTest {
    @Test
    fun testVoiceSessionFlow() {
        val session = NexusVoiceSessionManager()
        session.startListening()
        val intent = session.processSpokenText("Open YouTube")
        assertEquals(VoiceListeningState.SPEAKING, session.listeningState.value)
        assertEquals("com.google.android.youtube.tv", intent.parameter)
        session.finishSpeaking()
        assertEquals(VoiceListeningState.IDLE, session.listeningState.value)
    }

    @Test
    fun testWakeWordStatusInspection() {
        val session = NexusVoiceSessionManager()
        assertFalse(session.wakeWordCapability.isSupportedOffline)
    }
}
