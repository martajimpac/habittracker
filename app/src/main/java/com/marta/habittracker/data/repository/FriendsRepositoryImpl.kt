package com.marta.habittracker.data.repository

import android.util.Log
import com.marta.habittracker.core.network.NetworkChecker
import com.marta.habittracker.data.mapper.HabitMapper
import com.marta.habittracker.data.mapper.SocialMapper
import com.marta.habittracker.data.remote.HabitRemoteMapper
import com.marta.habittracker.data.remote.SocialRemoteDataSource
import com.marta.habittracker.data.remote.dto.HabitDto
import com.marta.habittracker.data.remote.dto.HabitRecordDto
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Challenge
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.Friendship
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.usecase.ComputeChallengeProgress
import com.marta.habittracker.domain.usecase.calculateScheduledStreak
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate

@Singleton
class FriendsRepositoryImpl @Inject constructor(
    private val networkChecker: NetworkChecker,
    private val socialRemote: SocialRemoteDataSource,
    private val socialMapper: SocialMapper,
    private val habitMapper: HabitMapper,
    private val computeChallengeProgress: ComputeChallengeProgress,
) : FriendsRepository {

    override suspend fun getAcceptedFriends(): DataResult<List<FriendListItem>, AppError> =
        runOnline(OP_ACCEPTED_FRIENDS) {
            val userId = socialRemote.requireUserId()
            val friendships = socialRemote.fetchFriendshipsForUser(userId, STATUS_ACCEPTED)
            val peerIds = friendships.map { friendship ->
                if (friendship.requesterId == userId) friendship.addresseeId else friendship.requesterId
            }.distinct()
            val profilesById = socialRemote.fetchProfilesByIds(peerIds)
                .associate { with(socialMapper) { it.toDomain() }.let { profile -> profile.id to profile } }
            val activeChallenges = socialRemote.fetchChallengesForUser(userId, listOf(STATUS_ACTIVE))
            val today = currentDate()

            peerIds.mapNotNull { peerId ->
                val profile = profilesById[peerId]
                if (profile == null) {
                    Log.w(TAG, "getAcceptedFriends missing profile peerId=$peerId")
                    return@mapNotNull null
                }
                val habitDtos = socialRemote.fetchPublicHabitsForUser(peerId)
                val records = socialRemote.fetchRecordsForHabits(habitDtos.map { it.id })
                val habits = mapHabits(habitDtos, records)
                FriendListItem(
                    profile = profile,
                    bestStreak = habits.maxOfOrNull { habit ->
                        calculateScheduledStreak(
                            daysOfWeek = habit.daysOfWeek,
                            records = habit.records,
                            today = today,
                            createdOn = LocalDate.parse(
                                habit.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString(),
                            ),
                        )
                    } ?: 0,
                    publicHabitsCount = habits.size,
                    activeChallengeCount = activeChallenges.count {
                        it.challengerId == peerId || it.challengedId == peerId
                    },
                )
            }
        }

    override suspend fun getActiveChallenges(): DataResult<List<ChallengeCard>, AppError> =
        runOnline(OP_ACTIVE_CHALLENGES) {
            val userId = socialRemote.requireUserId()
            val challengeDtos = socialRemote.fetchChallengesForUser(
                userId = userId,
                statuses = listOf(STATUS_PENDING, STATUS_ACTIVE),
            )
            val challenges = challengeDtos.map { with(socialMapper) { it.toDomain() } }
            val opponentIds = challenges.map { challenge ->
                if (challenge.challengerId == userId) challenge.challengedId else challenge.challengerId
            }.distinct()
            val profilesById = socialRemote.fetchProfilesByIds(opponentIds)
                .associate { with(socialMapper) { it.toDomain() }.let { profile -> profile.id to profile } }
            val habitIds = challenges.flatMap {
                listOf(it.challengerHabitId, it.challengedHabitId)
            }.distinct()
            val habits = mapHabits(
                habitDtos = socialRemote.fetchHabitsByIds(habitIds),
                recordDtos = socialRemote.fetchRecordsForHabits(habitIds),
            ).associateBy { it.id }
            val today = currentDate()

            challenges.mapNotNull { challenge ->
                toCard(challenge, userId, profilesById, habits, today) ?: run {
                    Log.w(TAG, "getActiveChallenges incomplete challengeId=${challenge.id}")
                    null
                }
            }
        }

    override suspend fun getChallenge(challengeId: String): DataResult<ChallengeCard, AppError> =
        when (val result = runOnline(OP_GET_CHALLENGE) {
            val userId = socialRemote.requireUserId()
            val dto = socialRemote.fetchChallengeById(challengeId)
                ?: return@runOnline SingleChallengeOutcome.NotFound
            val card = buildChallengeCard(with(socialMapper) { dto.toDomain() }, userId)
            if (card == null) {
                SingleChallengeOutcome.Incomplete
            } else {
                SingleChallengeOutcome.Success(card)
            }
        }) {
            is DataResult.Success -> when (val outcome = result.data) {
                is SingleChallengeOutcome.Success -> DataResult.Success(outcome.card)
                SingleChallengeOutcome.NotFound -> DataResult.Error(AppError.Common.Unknown)
                SingleChallengeOutcome.Incomplete -> {
                    Log.w(TAG, "getChallenge incomplete challengeId=$challengeId")
                    DataResult.Error(AppError.Common.Unknown)
                }
            }
            is DataResult.Error -> result
        }

    override suspend fun respondToChallenge(
        challengeId: String,
        accept: Boolean,
    ): DataResult<ChallengeCard, AppError> =
        when (val result = runOnline(OP_RESPOND_CHALLENGE) {
            val userId = socialRemote.requireUserId()
            val existing = socialRemote.fetchChallengeById(challengeId)
            if (existing == null ||
                existing.challengedId != userId ||
                existing.status != STATUS_PENDING
            ) {
                return@runOnline RespondChallengeOutcome.NotAllowed
            }
            val updated = socialRemote.updateChallengeStatus(
                id = challengeId,
                status = if (accept) STATUS_ACTIVE else STATUS_DECLINED,
            )
            val card = buildChallengeCard(with(socialMapper) { updated.toDomain() }, userId)
            if (card == null) {
                RespondChallengeOutcome.Incomplete
            } else {
                RespondChallengeOutcome.Success(card)
            }
        }) {
            is DataResult.Success -> when (val outcome = result.data) {
                is RespondChallengeOutcome.Success -> DataResult.Success(outcome.card)
                RespondChallengeOutcome.NotAllowed -> DataResult.Error(AppError.Common.Unknown)
                RespondChallengeOutcome.Incomplete -> {
                    Log.w(TAG, "respondToChallenge incomplete challengeId=$challengeId")
                    DataResult.Error(AppError.Common.Unknown)
                }
            }
            is DataResult.Error -> result
        }

    override suspend fun getPendingFriendRequests(): DataResult<List<Friendship>, AppError> =
        runOnline(OP_PENDING_REQUESTS) {
            val userId = socialRemote.requireUserId()
            socialRemote.fetchFriendshipsForUser(userId, STATUS_PENDING)
                .filter { it.addresseeId == userId }
                .map { with(socialMapper) { it.toDomain() } }
        }

    override suspend fun searchProfiles(query: String): DataResult<List<Profile>, AppError> =
        runOnline(OP_SEARCH_PROFILES) {
            val userId = socialRemote.requireUserId()
            socialRemote.searchProfilesByUsername(query)
                .filterNot { it.id == userId }
                .map { with(socialMapper) { it.toDomain() } }
        }

    override suspend fun sendFriendRequest(addresseeId: String): DataResult<Unit, AppError> =
        runOnline(OP_SEND_REQUEST) {
            val userId = socialRemote.requireUserId()
            socialRemote.insertFriendRequest(userId, addresseeId)
        }

    override suspend fun respondToFriendRequest(
        friendshipId: String,
        accept: Boolean,
    ): DataResult<Unit, AppError> = runOnline(OP_RESPOND_REQUEST) {
        socialRemote.updateFriendshipStatus(
            id = friendshipId,
            status = if (accept) STATUS_ACCEPTED else STATUS_REJECTED,
        )
    }

    override suspend fun getPublicHabitsForFriend(
        friendUserId: String,
    ): DataResult<List<Habit>, AppError> = runOnline(OP_PUBLIC_HABITS) {
        val habitDtos = socialRemote.fetchPublicHabitsForUser(friendUserId)
        val records = socialRemote.fetchRecordsForHabits(habitDtos.map { it.id })
        mapHabits(habitDtos, records)
    }

    override suspend fun createChallenge(
        challengedId: String,
        challengerHabitId: String,
        challengedHabitId: String,
        criteria: ChallengeCriteria,
        durationDays: Int,
    ): DataResult<Unit, AppError> = runOnline(OP_CREATE_CHALLENGE) {
        require(durationDays > 0) { "Challenge duration must be positive" }
        val startsAt = Instant.now()
        socialRemote.insertChallenge(
            challengerId = socialRemote.requireUserId(),
            challengedId = challengedId,
            challengerHabitId = challengerHabitId,
            challengedHabitId = challengedHabitId,
            criteria = criteria.toRemoteValue(),
            startsAt = startsAt,
            endsAt = startsAt.plus(durationDays.toLong(), ChronoUnit.DAYS),
        )
    }

    private suspend fun buildChallengeCard(
        challenge: Challenge,
        userId: String,
    ): ChallengeCard? {
        val opponentId = if (challenge.challengerId == userId) {
            challenge.challengedId
        } else {
            challenge.challengerId
        }
        val profilesById = socialRemote.fetchProfilesByIds(listOf(opponentId))
            .associate { with(socialMapper) { it.toDomain() }.let { profile -> profile.id to profile } }
        val habitIds = listOf(challenge.challengerHabitId, challenge.challengedHabitId)
        val habits = mapHabits(
            habitDtos = socialRemote.fetchHabitsByIds(habitIds),
            recordDtos = socialRemote.fetchRecordsForHabits(habitIds),
        ).associateBy { it.id }
        return toCard(challenge, userId, profilesById, habits, currentDate())
    }

    private fun toCard(
        challenge: Challenge,
        userId: String,
        profilesById: Map<String, Profile>,
        habits: Map<String, Habit>,
        today: LocalDate,
    ): ChallengeCard? {
        val isChallenger = challenge.challengerId == userId
        val opponentId = if (isChallenger) challenge.challengedId else challenge.challengerId
        val myHabitId = if (isChallenger) challenge.challengerHabitId else challenge.challengedHabitId
        val theirHabitId = if (isChallenger) challenge.challengedHabitId else challenge.challengerHabitId
        val opponent = profilesById[opponentId]
        val myHabit = habits[myHabitId]
        val theirHabit = habits[theirHabitId]
        if (opponent == null || myHabit == null || theirHabit == null) {
            return null
        }

        return ChallengeCard(
            challenge = challenge,
            opponent = opponent,
            myProgress = computeChallengeProgress(
                challenge.criteria,
                challenge.startsAt,
                challenge.endsAt,
                myHabit.records,
                today,
            ),
            theirProgress = computeChallengeProgress(
                challenge.criteria,
                challenge.startsAt,
                challenge.endsAt,
                theirHabit.records,
                today,
            ),
            daysLeft = computeChallengeProgress.daysLeft(
                challenge.startsAt,
                challenge.endsAt,
                today,
            ),
            habitName = myHabit.name,
            habitIcon = myHabit.icon,
            habitColorHex = myHabit.colorHex,
        )
    }

    private fun mapHabits(
        habitDtos: List<HabitDto>,
        recordDtos: List<HabitRecordDto>,
    ): List<Habit> {
        val recordsByHabit = recordDtos
            .map { dto ->
                val entity = with(HabitRemoteMapper) { dto.toEntity() }
                with(habitMapper) { entity.toDomain() }
            }
            .groupBy { it.habitId }

        return habitDtos.map { dto ->
            val entity = with(HabitRemoteMapper) { dto.toEntity() }
            with(habitMapper) { entity.toDomain() }
                .copy(records = recordsByHabit[dto.id].orEmpty())
        }
    }

    private suspend fun <T> runOnline(
        operation: String,
        block: suspend () -> T,
    ): DataResult<T, AppError> {
        if (!networkChecker.isOnline()) {
            Log.w(TAG, "$operation skipped: offline")
            return DataResult.Error(AppError.Common.Network)
        }
        return try {
            Log.d(TAG, "$operation started")
            val result = block()
            Log.d(TAG, "$operation finished")
            DataResult.Success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$operation failed", e)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    private fun ChallengeCriteria.toRemoteValue(): String = when (this) {
        ChallengeCriteria.Streak -> "streak"
        ChallengeCriteria.AllDays -> "all_days"
        ChallengeCriteria.CompletionPct -> "completion_pct"
    }

    private fun currentDate(): LocalDate = LocalDate.parse(java.time.LocalDate.now().toString())

    private sealed interface SingleChallengeOutcome {
        data class Success(val card: ChallengeCard) : SingleChallengeOutcome
        data object NotFound : SingleChallengeOutcome
        data object Incomplete : SingleChallengeOutcome
    }

    private sealed interface RespondChallengeOutcome {
        data class Success(val card: ChallengeCard) : RespondChallengeOutcome
        data object NotAllowed : RespondChallengeOutcome
        data object Incomplete : RespondChallengeOutcome
    }

    companion object {
        private const val TAG = "FriendsRepository"
        private const val STATUS_PENDING = "pending"
        private const val STATUS_ACCEPTED = "accepted"
        private const val STATUS_REJECTED = "rejected"
        private const val STATUS_ACTIVE = "active"
        private const val STATUS_DECLINED = "declined"
        private const val OP_ACCEPTED_FRIENDS = "getAcceptedFriends"
        private const val OP_ACTIVE_CHALLENGES = "getActiveChallenges"
        private const val OP_GET_CHALLENGE = "getChallenge"
        private const val OP_RESPOND_CHALLENGE = "respondToChallenge"
        private const val OP_PENDING_REQUESTS = "getPendingFriendRequests"
        private const val OP_SEARCH_PROFILES = "searchProfiles"
        private const val OP_SEND_REQUEST = "sendFriendRequest"
        private const val OP_RESPOND_REQUEST = "respondToFriendRequest"
        private const val OP_PUBLIC_HABITS = "getPublicHabitsForFriend"
        private const val OP_CREATE_CHALLENGE = "createChallenge"
    }
}
