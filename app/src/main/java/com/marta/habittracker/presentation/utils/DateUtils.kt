package com.marta.habittracker.presentation.utils

import androidx.annotation.StringRes
import com.marta.habittracker.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

fun getCurrentWeekDays(today: LocalDate = LocalDate.now()): List<LocalDate> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { monday.plusDays(it.toLong()) }
}


fun dayShortLabel(date: LocalDate): String {
    val locale = Locale.getDefault()
    return date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

fun dayLabel(date: LocalDate): String {
    val locale = Locale.getDefault()
    return date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

@StringRes
fun greetingForHour(hour: Int): Int = when {
    hour < 14 -> R.string.home_greeting_morning
    hour < 21 -> R.string.home_greeting_afternoon
    else -> R.string.home_greeting_evening
}
