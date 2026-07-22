package com.marta.habittracker.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import java.time.Instant

data class Habit(
    val id: String,
    val name: String,
    val description: String?,
    val daysOfWeek: Set<DayOfWeek>,
    val icon: String,
    val colorHex: String,
    val reminderTime: String?,
    val createdAt: Instant,
    val records: List<HabitRecord>,
    val isCompleted: Boolean = false,
)

data class HabitRecord(
    val habitId: String,
    val date: LocalDate,
    val isCompleted: Boolean,
)
