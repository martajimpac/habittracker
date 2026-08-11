package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.Friendship
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.Profile

interface FriendsRepository {
    suspend fun getAcceptedFriends(): DataResult<List<FriendListItem>, AppError>

    suspend fun getActiveChallenges(): DataResult<List<ChallengeCard>, AppError>

    suspend fun getPendingFriendRequests(): DataResult<List<Friendship>, AppError>

    suspend fun searchProfiles(query: String): DataResult<List<Profile>, AppError>

    suspend fun sendFriendRequest(addresseeId: String): DataResult<Unit, AppError>

    suspend fun respondToFriendRequest(
        friendshipId: String,
        accept: Boolean,
    ): DataResult<Unit, AppError>

    suspend fun getPublicHabitsForFriend(friendUserId: String): DataResult<List<Habit>, AppError>

    suspend fun createChallenge(
        challengedId: String,
        challengerHabitId: String,
        challengedHabitId: String,
        criteria: ChallengeCriteria,
        durationDays: Int,
    ): DataResult<Unit, AppError>
}
