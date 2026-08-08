package com.marta.habittracker.domain.model

import java.time.Instant

enum class ChallengeStatus {
    Pending,
    Active,
    Completed,
    Declined,
    Cancelled,
}

data class Challenge(
    val id: String,
    val challengerId: String,
    val challengedId: String,
    val challengerHabitId: String,
    val challengedHabitId: String,
    val criteria: ChallengeCriteria,
    val startsAt: Instant,
    val endsAt: Instant,
    val status: ChallengeStatus,
)

/** Snapshot for the Friends active-challenges carousel (progress computed at load time). */
data class ChallengeCard(
    val challenge: Challenge,
    val opponent: Profile,
    val myProgress: Int,
    val theirProgress: Int,
    val daysLeft: Int,
    val habitName: String,
    val habitIcon: String,
    val habitColorHex: String,
)
