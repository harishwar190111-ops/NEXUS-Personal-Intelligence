package com.nexus.personalintelligence.mobile.assistant

import com.nexus.personalintelligence.mobile.ai.ProviderSettings
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCommandRouterTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-01T14:05:00Z"), ZoneOffset.UTC)
    private val router = LocalCommandRouter(clock, Locale.US)
    private val ctx = AssistantContext(ProviderSettings())

    @Test
    fun time_and_date_useTheClock() {
        assertEquals("It is 14:05.", router.reply("What time is it?", ctx))
        assertEquals("Today is Sunday, 1 November 2026.", router.reply("what's the date", ctx))
    }

    @Test
    fun tv_isHonestAboutNoPairing() {
        assertTrue(router.reply("is my tv connected", ctx).startsWith("No TV is paired"))
    }

    @Test
    fun provider_reportsConfigStatus() {
        assertTrue(router.reply("ai status", ctx).contains("No AI provider selected"))
    }

    @Test
    fun memory_isNotImplemented() {
        assertTrue(router.reply("remember my keys", ctx).contains("not implemented"))
    }

    @Test
    fun unknown_neverPretendsToUnderstand() {
        assertTrue(router.reply("write me a poem", ctx).contains("don't understand"))
        assertTrue(router.reply("   ", ctx).startsWith("Type a command"))
        assertEquals(LocalCommandRouter.HELP, router.reply("help", ctx))
    }
}
