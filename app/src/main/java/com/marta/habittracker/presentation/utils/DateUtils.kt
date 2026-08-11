package com.marta.habittracker.presentation.utils

import androidx.annotation.StringRes
import com.marta.habittracker.R
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

@StringRes
fun greetingForHour(hour: Int): Int = when {
    hour < 12 -> R.string.home_greeting_morning
    hour < 17 -> R.string.home_greeting_afternoon
    else -> R.string.home_greeting_evening
}
