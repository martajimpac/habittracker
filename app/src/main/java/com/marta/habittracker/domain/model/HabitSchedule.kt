package com.marta.habittracker.domain.model

enum class HabitSchedule {
    DAILY,
    WEEKDAYS,
    WEEKLY_COUNT,
}

fun Habit.resolvedSchedule(): HabitSchedule = when (scheduleMode) {
    "daily" -> HabitSchedule.DAILY
    "weekdays" -> HabitSchedule.WEEKDAYS
    "weekly_count" -> HabitSchedule.WEEKLY_COUNT
    else -> when {
        daysOfWeek.size in 1..6 -> HabitSchedule.WEEKDAYS
        daysPerWeek in 1..6 -> HabitSchedule.WEEKLY_COUNT
        else -> HabitSchedule.DAILY
    }
}

fun HabitSchedule.storedValue(): String = when (this) {
    HabitSchedule.DAILY -> "daily"
    HabitSchedule.WEEKDAYS -> "weekdays"
    HabitSchedule.WEEKLY_COUNT -> "weekly_count"
}

fun HabitSchedule.protectionMilestone(): Int = when (this) {
    HabitSchedule.WEEKLY_COUNT -> 4
    HabitSchedule.DAILY, HabitSchedule.WEEKDAYS -> 7
}

fun Habit.countsInWeeks(): Boolean = resolvedSchedule() == HabitSchedule.WEEKLY_COUNT
