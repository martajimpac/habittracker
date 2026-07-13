package com.marta.habittracker.domain.models

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import java.time.Instant

data class HabitWithStatus(
    val id: Long,
    val name: String,
    val description: String?,
    val daysOfWeek: Set<DayOfWeek>,
    val createdAt: Instant,
    val records: List<HabitRecord>,
    val isCompleted: Boolean = false
)

data class HabitRecord(
    val habitId: Long,
    val date: LocalDate,
    val isCompleted: Boolean
)