package com.marta.habittracker.domain.model

fun ownChallengeHabitId(
    userId: String,
    challengerId: String,
    challengedId: String,
    challengerHabitId: String,
    challengedHabitId: String?,
): String? = when (userId) {
    challengerId -> challengerHabitId
    challengedId -> challengedHabitId
    else -> null
}
