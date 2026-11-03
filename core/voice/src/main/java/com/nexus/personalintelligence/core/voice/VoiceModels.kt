package com.nexus.personalintelligence.core.voice

import com.nexus.personalintelligence.core.ai.NexusAiEngine
import com.nexus.personalintelligence.core.ai.RecognizedAiIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class VoiceListeningState { IDLE, LISTENING, PROCESSING, SPEAKING, ERROR }

@Serializable
data class VoiceInteractionEntry(
    val id: String = UUID.randomUUID().toString(),
    val query: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class WakeWordCapability(
    val isSupportedOffline: Boolean = false,
    val engineName: String = "Porcupine / Snowboy Fallback Evaluator",
    val statusMessage: String = "Push-to-talk is primary supported mode on Galaxy F54."
)

class NexusVoiceSessionManager(
    private val aiEngine: NexusAiEngine = NexusAiEngine()
) {
    private val _listeningState = MutableStateFlow(VoiceListeningState.IDLE)
    val listeningState: StateFlow<VoiceListeningState> = _listeningState.asStateFlow()

    private val _conversationHistory = MutableStateFlow<List<VoiceInteractionEntry>>(emptyList())
    val conversationHistory: StateFlow<List<VoiceInteractionEntry>> = _conversationHistory.asStateFlow()

    val wakeWordCapability = WakeWordCapability()

    fun startListening() { _listeningState.value = VoiceListeningState.LISTENING }
    fun stopListening() { _listeningState.value = VoiceListeningState.PROCESSING }
    fun cancelListening() { _listeningState.value = VoiceListeningState.IDLE }

    fun processSpokenText(spokenText: String): RecognizedAiIntent {
        val recognized = aiEngine.parseDeterministicIntent(spokenText)
        val entry = VoiceInteractionEntry(query = spokenText, response = "Executing ${recognized.action}")
        _conversationHistory.value = _conversationHistory.value + entry
        _listeningState.value = VoiceListeningState.SPEAKING
        return recognized
    }

    fun finishSpeaking() { _listeningState.value = VoiceListeningState.IDLE }
}
