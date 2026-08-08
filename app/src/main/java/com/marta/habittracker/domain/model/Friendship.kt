package com.marta.habittracker.domain.model

enum class FriendshipStatus {
    Pending,
    Accepted,
    Rejected,
}

data class Friendship(
    val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: FriendshipStatus,
)
