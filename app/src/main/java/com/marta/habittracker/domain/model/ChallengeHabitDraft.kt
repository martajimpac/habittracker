package com.marta.habittracker.domain.model

import kotlinx.datetime.DayOfWeek
import java.time.Instant

data class ChallengeHabitDraft(
    val name: String,
    val icon: String,
    val colorHex: String,
    val daysOfWeek: Set<DayOfWeek>,
)

fun newChallengeHabit(
    id: String,
    draft: ChallengeHabitDraft,
    createdAt: Instant,
): Habit = Habit(
    id = id,
    name = draft.name.trim(),
    description = null,
    daysOfWeek = draft.daysOfWeek,
    icon = draft.icon,
    colorHex = draft.colorHex,
    reminderTime = null,
    createdAt = createdAt,
    records = emptyList(),
    isPublic = true,
    reminderEnabled = false,
)

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
    ),
    createdAt = createdAt,
)
