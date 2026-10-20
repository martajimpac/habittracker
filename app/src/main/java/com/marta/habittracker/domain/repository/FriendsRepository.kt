package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeHabitDraft
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.PendingFriendRequest
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.Profile

interface FriendsRepository {
    suspend fun getAcceptedFriends(): DataResult<List<FriendListItem>, AppError>

    suspend fun getActiveChallenges(): DataResult<List<ChallengeCard>, AppError>

    suspend fun challengeHabitIds(): DataResult<Set<String>, AppError>

    suspend fun getChallenge(challengeId: String): DataResult<ChallengeCard, AppError>

    suspend fun respondToChallenge(
        challengeId: String,
        accept: Boolean,
    ): DataResult<ChallengeCard, AppError>

    suspend fun getPendingFriendRequests(): DataResult<List<PendingFriendRequest>, AppError>

    suspend fun searchProfiles(query: String): DataResult<List<Profile>, AppError>

    suspend fun sendFriendRequest(addresseeId: String): DataResult<Unit, AppError>

    suspend fun respondToFriendRequest(
        friendshipId: String,
        accept: Boolean,
    ): DataResult<Unit, AppError>

    suspend fun getPublicHabitsForFriend(friendUserId: String): DataResult<List<Habit>, AppError>

    suspend fun createChallenge(
        challengedId: String,
        draft: ChallengeHabitDraft,
        criteria: ChallengeCriteria,
        durationDays: Int,
    ): DataResult<Unit, AppError>
}
