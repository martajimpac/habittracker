package com.marta.habittracker.domain.model

private const val UNSCHEDULED_TIME = "99:99"

/** Reminders first, by time. Habits without a reminder stay last, still ordered by time. */
fun List<Habit>.sortedForSchedule(): List<Habit> =
    sortedWith(
        compareBy<Habit> { habit -> if (habit.reminderEnabled) 0 else 1 }
            .thenBy { habit -> habit.reminderTime?.takeIf { it.isNotBlank() } ?: UNSCHEDULED_TIME }
            .thenBy { habit -> habit.name.lowercase() },
    )
