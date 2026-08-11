package com.marta.habittracker.domain.model

data class FriendListItem(
    val profile: Profile,
    val bestStreak: Int,
    val publicHabitsCount: Int,
    val activeChallengeCount: Int,
)
