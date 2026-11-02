package com.nexus.personalintelligence.core.common.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pure time/date helpers shared by both apps (free of Android APIs so they are unit-testable). */
object NexusTime {
    fun greetingFor(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello"
    }

    fun formatTime(time: LocalTime, use24Hour: Boolean, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", locale).format(time)

    fun formatDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(date)

    fun formatDateWithYear(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale).format(date)
}
