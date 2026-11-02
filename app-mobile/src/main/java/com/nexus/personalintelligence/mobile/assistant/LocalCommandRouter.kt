package com.nexus.personalintelligence.mobile.assistant

import com.nexus.personalintelligence.core.common.util.NexusTime
import com.nexus.personalintelligence.mobile.ai.ProviderSettings
import com.nexus.personalintelligence.mobile.ai.providerStatus
import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AssistantContext(val provider: ProviderSettings, val tvPaired: Boolean = false)

/**
 * Deterministic local commands. This is NOT an AI: it matches a few keywords and otherwise says it does not understand.
 */
class LocalCommandRouter(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val locale: Locale = Locale.getDefault(),
) {
    fun reply(input: String, ctx: AssistantContext): String {
        val words = input.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }.toSet()
        val now = ZonedDateTime.now(clock)
        return when {
            words.isEmpty() -> "Type a command. Try \"help\"."
            "help" in words || "commands" in words -> HELP
            "time" in words -> "It is ${DateTimeFormatter.ofPattern("HH:mm", locale).format(now)}."
            words.any { it in DATE_WORDS } -> "Today is ${NexusTime.formatDateWithYear(now.toLocalDate(), locale)}."
            words.any { it in TV_WORDS } ->
                if (ctx.tvPaired) "A TV is paired." else "No TV is paired. Pairing arrives in Release 2, so there is nothing to control yet."
            words.any { it in AI_WORDS } -> providerStatus(ctx.provider)
            words.any { it in MEMORY_WORDS } -> "Memory and reminders are not implemented yet (Release 4). Nothing you type here is stored."
            else -> "I don't understand that yet. This build has only a few local commands and no AI model connected. Type \"help\"."
        }
    }

    companion object {
        const val HELP = "Commands: time, date, tv (device status), ai (provider status), memory. No AI model is connected in this build."
        private val DATE_WORDS = setOf("date", "today", "day")
        private val TV_WORDS = setOf("tv", "television", "device", "devices")
        private val AI_WORDS = setOf("ai", "provider", "model", "gemini", "openai", "claude")
        private val MEMORY_WORDS = setOf("memory", "remember", "remind", "reminder", "note")
    }
}
