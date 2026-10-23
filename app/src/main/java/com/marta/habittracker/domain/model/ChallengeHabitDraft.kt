package com.marta.habittracker.domain.model

import kotlinx.datetime.DayOfWeek
import java.time.Instant

data class ChallengeHabitDraft(
    val name: String,
    val icon: String,
    val colorHex: String,
    val daysOfWeek: Set<DayOfWeek>,
    val daysPerWeek: Int = 7,
    val scheduleMode: String = "",
)

fun newChallengeHabit(
    id: String,
    draft: ChallengeHabitDraft,
    createdAt: Instant,
): Habit {
    val schedule = draft.scheduleMode.ifBlank {
        when {
            draft.daysOfWeek.size in 1..6 -> "weekdays"
            draft.daysPerWeek in 1..6 -> "weekly_count"
            else -> "daily"
        }
    }
    val days = if (schedule == "weekdays" && draft.daysOfWeek.isNotEmpty()) {
        draft.daysOfWeek
    } else {
        DayOfWeek.entries.toSet()
    }
    val perWeek = if (schedule == "weekly_count") draft.daysPerWeek.coerceIn(1, 7) else 7
    return Habit(
        id = id,
        name = draft.name.trim(),
        description = null,
        daysOfWeek = days,
        icon = draft.icon,
        colorHex = draft.colorHex,
        reminderTime = null,
        createdAt = createdAt,
        records = emptyList(),
        isPublic = true,
        reminderEnabled = false,
        daysPerWeek = perWeek,
        scheduleMode = schedule,
    )
}

fun copyChallengeHabit(
    source: Habit,
    id: String,
    createdAt: Instant,
): Habit = newChallengeHabit(
    id = id,
    draft = ChallengeHabitDraft(
        name = source.name,
        icon = source.icon,
        colorHex = source.colorHex,
        daysOfWeek = source.daysOfWeek,
        daysPerWeek = source.daysPerWeek,
        scheduleMode = source.scheduleMode,
    ),
    createdAt = createdAt,
)
