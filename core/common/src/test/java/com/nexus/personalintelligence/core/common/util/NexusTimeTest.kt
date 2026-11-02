package com.nexus.personalintelligence.core.common.util

import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class NexusTimeTest {
    @Test
    fun greeting_followsTimeOfDay() {
        assertEquals("Good morning", NexusTime.greetingFor(8))
        assertEquals("Good afternoon", NexusTime.greetingFor(13))
        assertEquals("Good evening", NexusTime.greetingFor(19))
        assertEquals("Hello", NexusTime.greetingFor(2))
    }

    @Test
    fun time_formats24And12Hour() {
        val t = LocalTime.of(14, 5)
        assertEquals("14:05", NexusTime.formatTime(t, true, Locale.US))
        assertEquals("2:05 PM", NexusTime.formatTime(t, false, Locale.US))
    }

    @Test
    fun date_formatsWeekdayDayMonth() {
        val d = LocalDate.of(2026, 11, 1)
        assertEquals("Sunday, 1 November", NexusTime.formatDate(d, Locale.US))
        assertEquals("Sunday, 1 November 2026", NexusTime.formatDateWithYear(d, Locale.US))
    }
}
