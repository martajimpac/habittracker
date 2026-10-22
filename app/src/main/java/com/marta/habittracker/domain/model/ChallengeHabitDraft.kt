package com.marta.habittracker.domain.model

import kotlinx.datetime.DayOfWeek
import java.time.Instant

data class ChallengeHabitDraft(
    val name: String,
    val icon: String,
    val colorHex: String,
    val daysOfWeek: Set<DayOfWeek>,
    val daysPerWeek: Int = 7,
)

fun newChallengeHabit(
    id: String,
    draft: ChallengeHabitDraft,
    createdAt: Instant,
): Habit {
    val explicit = draft.daysPerWeek.coerceIn(1, 7)
    val target = if (explicit < 7) {
        explicit
    } else if (draft.daysOfWeek.size in 1..6) {
        draft.daysOfWeek.size
    } else {
        7
    }
    return Habit(
        id = id,
        name = draft.name.trim(),
        description = null,
        daysOfWeek = DayOfWeek.entries.toSet(),
        icon = draft.icon,
        colorHex = draft.colorHex,
        reminderTime = null,
        createdAt = createdAt,
        records = emptyList(),
        isPublic = true,
        reminderEnabled = false,
        daysPerWeek = target,
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
    ),
    createdAt = createdAt,
)
