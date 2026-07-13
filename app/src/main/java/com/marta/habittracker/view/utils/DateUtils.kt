package com.marta.habittracker.view.utils

import java.time.LocalDate

fun getCalendarDays(): List<LocalDate> {
    val today = LocalDate.now()
    // Genera 3 días antes y 3 días después de hoy
    return (-3..3).map { today.plusDays(it.toLong()) }
}