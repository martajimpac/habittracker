package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeHabitDraft
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.PendingFriendRequest
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.repository.FriendsRepository

class FakeFriendsRepository(
    private val acceptedFriendsResult: DataResult<List<FriendListItem>, AppError> =
        DataResult.Success(emptyList()),
    var activeChallengesResult: DataResult<List<ChallengeCard>, AppError> =
        DataResult.Success(emptyList()),
    private val getChallengeResult: DataResult<ChallengeCard, AppError> =
        DataResult.Error(AppError.Common.Unknown),
    private val respondToChallengeResult: DataResult<ChallengeCard, AppError> =
        DataResult.Error(AppError.Common.Unknown),
    private val pendingFriendRequestsResult: DataResult<List<PendingFriendRequest>, AppError> =
        DataResult.Success(emptyList()),
    private val searchProfilesResult: DataResult<List<Profile>, AppError> =
        DataResult.Success(emptyList()),
    private val sendFriendRequestResult: DataResult<Unit, AppError> = DataResult.Success(Unit),
    private val respondToFriendRequestResult: DataResult<Unit, AppError> = DataResult.Success(Unit),
    private val publicHabitsResult: DataResult<List<Habit>, AppError> = DataResult.Success(emptyList()),
    private val createChallengeResult: DataResult<Unit, AppError> = DataResult.Success(Unit),
    private val cancelChallengeResult: DataResult<Unit, AppError> = DataResult.Success(Unit),
    private val renameChallengeResult: DataResult<ChallengeCard, AppError> =
        DataResult.Error(AppError.Common.Unknown),
) : FriendsRepository {

    var getAcceptedFriendsCalls: Int = 0
        private set
    var getActiveChallengesCalls: Int = 0
        private set
    var respondCalls: Int = 0
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
    var cancelChallengeCalls: Int = 0
        private set
    var renameChallengeCalls: Int = 0
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
    var lastDraft: ChallengeHabitDraft? = null
        private set
    var lastChallengeCriteria: ChallengeCriteria? = null
        private set
    var lastDurationDays: Int? = null
        private set
    var lastCancelChallengeId: String? = null
        private set
    var lastChallengeName: String? = null
        private set

    override suspend fun getAcceptedFriends(): DataResult<List<FriendListItem>, AppError> {
        getAcceptedFriendsCalls++
        return acceptedFriendsResult
    }

    override suspend fun getActiveChallenges(): DataResult<List<ChallengeCard>, AppError> {
        getActiveChallengesCalls++
        return activeChallengesResult
    }

    override suspend fun getChallenge(challengeId: String): DataResult<ChallengeCard, AppError> =
        getChallengeResult

    override suspend fun respondToChallenge(
        challengeId: String,
        accept: Boolean,
    ): DataResult<ChallengeCard, AppError> {
        respondCalls++
        lastRespondAccept = accept
        return respondToChallengeResult
    }

    override suspend fun getPendingFriendRequests(): DataResult<List<PendingFriendRequest>, AppError> {
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
        draft: ChallengeHabitDraft,
        criteria: ChallengeCriteria,
        durationDays: Int,
    ): DataResult<Unit, AppError> {
        createChallengeCalls++
        lastChallengedId = challengedId
        lastDraft = draft
        lastChallengeCriteria = criteria
        lastDurationDays = durationDays
        return createChallengeResult
    }

    override suspend fun cancelChallenge(challengeId: String): DataResult<Unit, AppError> {
        cancelChallengeCalls++
        lastCancelChallengeId = challengeId
        return cancelChallengeResult
    }

    override suspend fun renameChallenge(
        challengeId: String,
        name: String,
    ): DataResult<ChallengeCard, AppError> {
        renameChallengeCalls++
        lastChallengeName = name
        return renameChallengeResult
    }

    var challengeHabitIdsResult: DataResult<Set<String>, AppError> = DataResult.Success(emptySet())

    override suspend fun challengeHabitIds(): DataResult<Set<String>, AppError> = challengeHabitIdsResult
}
