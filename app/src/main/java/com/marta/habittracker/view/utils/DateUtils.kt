package com.marta.habittracker.view.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

fun getCalendarDays(): List<LocalDate> {
    val today = LocalDate.now()
    return (-3..3).map { today.plusDays(it.toLong()) }
}

fun getCurrentWeekDays(): List<LocalDate> {
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { monday.plusDays(it.toLong()) }
}

fun dayShortLabel(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

fun isSameDay(a: LocalDate, b: LocalDate): Boolean = a == b

fun greetingForHour(hour: Int): String = when {
    hour < 12 -> "Good morning"
    hour < 17 -> "Good afternoon"
    else -> "Good evening"
}
