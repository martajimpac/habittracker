package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.Friendship
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.repository.FriendsRepository

class FakeFriendsRepository(
    private val acceptedFriendsResult: DataResult<List<FriendListItem>, AppError> =
        DataResult.Success(emptyList()),
    private val activeChallengesResult: DataResult<List<ChallengeCard>, AppError> =
        DataResult.Success(emptyList()),
    private val pendingFriendRequestsResult: DataResult<List<Friendship>, AppError> =
        DataResult.Success(emptyList()),
    private val searchProfilesResult: DataResult<List<Profile>, AppError> =
        DataResult.Success(emptyList()),
    private val sendFriendRequestResult: DataResult<Unit, AppError> = DataResult.Success(Unit),
    private val respondToFriendRequestResult: DataResult<Unit, AppError> = DataResult.Success(Unit),
    private val publicHabitsResult: DataResult<List<Habit>, AppError> = DataResult.Success(emptyList()),
    private val createChallengeResult: DataResult<Unit, AppError> = DataResult.Success(Unit),
) : FriendsRepository {

    var getAcceptedFriendsCalls: Int = 0
        private set
    var getActiveChallengesCalls: Int = 0
        private set
    var getPendingFriendRequestsCalls: Int = 0
        private set
    var searchProfilesCalls: Int = 0
        private set
    var sendFriendRequestCalls: Int = 0
        private set
    var respondToFriendRequestCalls: Int = 0
        private set
    var getPublicHabitsForFriendCalls: Int = 0
        private set
    var createChallengeCalls: Int = 0
        private set

    var lastSearchQuery: String? = null
        private set
    var lastAddresseeId: String? = null
        private set
    var lastFriendshipId: String? = null
        private set
    var lastRespondAccept: Boolean? = null
        private set
    var lastFriendUserId: String? = null
        private set
    var lastChallengedId: String? = null
        private set
    var lastChallengerHabitId: String? = null
        private set
    var lastChallengedHabitId: String? = null
        private set
    var lastChallengeCriteria: ChallengeCriteria? = null
        private set
    var lastDurationDays: Int? = null
        private set

    override suspend fun getAcceptedFriends(): DataResult<List<FriendListItem>, AppError> {
        getAcceptedFriendsCalls++
        return acceptedFriendsResult
    }

    override suspend fun getActiveChallenges(): DataResult<List<ChallengeCard>, AppError> {
        getActiveChallengesCalls++
        return activeChallengesResult
    }

    override suspend fun getPendingFriendRequests(): DataResult<List<Friendship>, AppError> {
        getPendingFriendRequestsCalls++
        return pendingFriendRequestsResult
    }

    override suspend fun searchProfiles(query: String): DataResult<List<Profile>, AppError> {
        searchProfilesCalls++
        lastSearchQuery = query
        return searchProfilesResult
    }

    override suspend fun sendFriendRequest(addresseeId: String): DataResult<Unit, AppError> {
        sendFriendRequestCalls++
        lastAddresseeId = addresseeId
        return sendFriendRequestResult
    }

    override suspend fun respondToFriendRequest(
        friendshipId: String,
        accept: Boolean,
    ): DataResult<Unit, AppError> {
        respondToFriendRequestCalls++
        lastFriendshipId = friendshipId
        lastRespondAccept = accept
        return respondToFriendRequestResult
    }

    override suspend fun getPublicHabitsForFriend(
        friendUserId: String,
    ): DataResult<List<Habit>, AppError> {
        getPublicHabitsForFriendCalls++
        lastFriendUserId = friendUserId
        return publicHabitsResult
    }

    override suspend fun createChallenge(
        challengedId: String,
        challengerHabitId: String,
        challengedHabitId: String,
        criteria: ChallengeCriteria,
        durationDays: Int,
    ): DataResult<Unit, AppError> {
        createChallengeCalls++
        lastChallengedId = challengedId
        lastChallengerHabitId = challengerHabitId
        lastChallengedHabitId = challengedHabitId
        lastChallengeCriteria = criteria
        lastDurationDays = durationDays
        return createChallengeResult
    }
}
