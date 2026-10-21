package com.marta.habittracker.data.repository

import android.util.Log
import com.marta.habittracker.core.IdGenerator
import com.marta.habittracker.core.network.NetworkChecker
import com.marta.habittracker.data.mapper.HabitMapper
import com.marta.habittracker.data.mapper.SocialMapper
import com.marta.habittracker.data.remote.HabitRemoteMapper
import com.marta.habittracker.data.remote.SocialRemoteDataSource
import com.marta.habittracker.data.remote.dto.ChallengeDto
import com.marta.habittracker.data.remote.dto.HabitDto
import com.marta.habittracker.data.remote.dto.HabitRecordDto
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Challenge
import com.marta.habittracker.domain.model.ChallengeCard
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeHabitDraft
import com.marta.habittracker.domain.model.completedDatesInChallenge
import com.marta.habittracker.domain.model.ownChallengeHabitId
import com.marta.habittracker.domain.model.ChallengeStatus
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.PendingFriendRequest
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.model.copyChallengeHabit
import com.marta.habittracker.domain.model.newChallengeHabit
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
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
    private val habitRepository: HabitRepository,
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
                listOfNotNull(it.challengerHabitId, it.challengedHabitId)
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

    override suspend fun challengeHabitIds(): DataResult<Set<String>, AppError> =
        runOnline(OP_CHALLENGE_HABIT_IDS) {
            val userId = socialRemote.requireUserId()
            socialRemote.fetchChallengesForUser(
                userId = userId,
                statuses = listOf(STATUS_PENDING, STATUS_ACTIVE),
            ).mapNotNull { challenge ->
                ownChallengeHabitId(
                    userId = userId,
                    challengerId = challenge.challengerId,
                    challengedId = challenge.challengedId,
                    challengerHabitId = challenge.challengerHabitId,
                    challengedHabitId = challenge.challengedHabitId,
                )
            }.toSet()
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
            val updated = if (accept) {
                acceptChallenge(existing)
            } else {
                socialRemote.updateChallengeStatus(
                    id = challengeId,
                    status = STATUS_DECLINED,
                )
            }
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

    override suspend fun cancelChallenge(challengeId: String): DataResult<Unit, AppError> =
        when (val result = runOnline(OP_CANCEL_CHALLENGE) {
            val userId = socialRemote.requireUserId()
            val existing = socialRemote.fetchChallengeById(challengeId)
            val allowed = existing != null &&
                (existing.challengerId == userId || existing.challengedId == userId) &&
                (existing.status == STATUS_PENDING || existing.status == STATUS_ACTIVE)
            if (!allowed) {
                false
            } else {
                socialRemote.updateChallengeStatus(challengeId, STATUS_CANCELLED)
                true
            }
        }) {
            is DataResult.Success -> if (result.data) {
                DataResult.Success(Unit)
            } else {
                Log.w(TAG, "cancelChallenge not allowed id=$challengeId")
                DataResult.Error(AppError.Common.Unknown)
            }
            is DataResult.Error -> result
        }

    override suspend fun renameChallenge(
        challengeId: String,
        name: String,
    ): DataResult<ChallengeCard, AppError> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            Log.w(TAG, "renameChallenge rejected: blank name id=$challengeId")
            return DataResult.Error(AppError.Common.Unknown)
        }
        return when (val result = runOnline(OP_RENAME_CHALLENGE) {
            val userId = socialRemote.requireUserId()
            val existing = socialRemote.fetchChallengeById(challengeId)
                ?: return@runOnline RenameChallengeOutcome.NotAllowed
            val habitId = ownChallengeHabitId(
                userId = userId,
                challengerId = existing.challengerId,
                challengedId = existing.challengedId,
                challengerHabitId = existing.challengerHabitId,
                challengedHabitId = existing.challengedHabitId,
            ) ?: return@runOnline RenameChallengeOutcome.NotAllowed
            val habitDto = socialRemote.fetchHabitsByIds(listOf(habitId)).firstOrNull()
                ?: return@runOnline RenameChallengeOutcome.NotAllowed
            val habit = mapHabits(listOf(habitDto), emptyList()).first().copy(name = trimmed)
            when (val updated = habitRepository.updateHabit(habit)) {
                is DataResult.Error -> return@runOnline RenameChallengeOutcome.Failed(updated.error)
                is DataResult.Success -> Unit
            }
            val card = buildChallengeCard(with(socialMapper) { existing.toDomain() }, userId)
                ?.copy(habitName = trimmed)
            if (card == null) {
                RenameChallengeOutcome.NotAllowed
            } else {
                RenameChallengeOutcome.Success(card)
            }
        }) {
            is DataResult.Success -> when (val outcome = result.data) {
                is RenameChallengeOutcome.Success -> DataResult.Success(outcome.card)
                RenameChallengeOutcome.NotAllowed -> {
                    Log.w(TAG, "renameChallenge not allowed id=$challengeId")
                    DataResult.Error(AppError.Common.Unknown)
                }
                is RenameChallengeOutcome.Failed -> DataResult.Error(outcome.error)
            }
            is DataResult.Error -> result
        }
    }

    override suspend fun getPendingFriendRequests(): DataResult<List<PendingFriendRequest>, AppError> =
        runOnline(OP_PENDING_REQUESTS) {
            val userId = socialRemote.requireUserId()
            val pending = socialRemote.fetchFriendshipsForUser(userId, STATUS_PENDING)
                .filter { it.addresseeId == userId }
            val profilesById = socialRemote.fetchProfilesByIds(pending.map { it.requesterId }.distinct())
                .associate { with(socialMapper) { it.toDomain() }.let { profile -> profile.id to profile } }
            pending.mapNotNull { friendship ->
                val profile = profilesById[friendship.requesterId]
                if (profile == null) {
                    Log.w(TAG, "getPendingFriendRequests missing profile requesterId=${friendship.requesterId}")
                    return@mapNotNull null
                }
                PendingFriendRequest(id = friendship.id, requester = profile)
            }
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
        draft: ChallengeHabitDraft,
        criteria: ChallengeCriteria,
        durationDays: Int,
    ): DataResult<Unit, AppError> {
        if (durationDays <= 0 || draft.name.isBlank() || draft.daysOfWeek.isEmpty()) {
            Log.w(TAG, "createChallenge rejected: invalid draft")
            return DataResult.Error(AppError.Common.Unknown)
        }
        val habit = newChallengeHabit(
            id = IdGenerator.newId(),
            draft = draft,
            createdAt = Instant.now(),
        )
        when (val inserted = habitRepository.insertHabit(habit)) {
            is DataResult.Error -> return inserted
            is DataResult.Success -> Unit
        }
        val created = runOnline(OP_CREATE_CHALLENGE) {
            val startsAt = Instant.now()
            socialRemote.insertChallenge(
                challengerId = socialRemote.requireUserId(),
                challengedId = challengedId,
                challengerHabitId = habit.id,
                challengedHabitId = null,
                criteria = criteria.toRemoteValue(),
                startsAt = startsAt,
                endsAt = startsAt.plus(durationDays.toLong(), ChronoUnit.DAYS),
            )
        }
        if (created is DataResult.Error) {
            Log.e(TAG, "createChallenge failed after habit insert id=${habit.id}")
            habitRepository.deleteHabit(habit)
            return created
        }
        return DataResult.Success(Unit)
    }

    private suspend fun acceptChallenge(existing: ChallengeDto): ChallengeDto {
        val sourceDto = socialRemote.fetchHabitsByIds(listOf(existing.challengerHabitId)).firstOrNull()
            ?: error("Challenge habit missing id=${existing.challengerHabitId}")
        val source = mapHabits(listOf(sourceDto), emptyList()).first()
        val copy = copyChallengeHabit(
            source = source,
            id = IdGenerator.newId(),
            createdAt = Instant.now(),
        )
        when (val inserted = habitRepository.insertHabit(copy)) {
            is DataResult.Error -> error("Challenge habit copy failed id=${copy.id}")
            is DataResult.Success -> Unit
        }
        return try {
            socialRemote.acceptChallenge(existing.id, copy.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "acceptChallenge failed, removing habit id=${copy.id}", e)
            habitRepository.deleteHabit(copy)
            throw e
        }
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
        val habitIds = listOfNotNull(challenge.challengerHabitId, challenge.challengedHabitId)
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
        val myHabit = myHabitId?.let { habits[it] }
        val theirHabit = theirHabitId?.let { habits[it] }
        val definedHabit = myHabit ?: theirHabit
        if (opponent == null || definedHabit == null || (isChallenger && myHabit == null)) {
            return null
        }
        if (!isChallenger && challenge.status != ChallengeStatus.Pending && myHabit == null) {
            return null
        }

        return ChallengeCard(
            challenge = challenge,
            opponent = opponent,
            myProgress = computeChallengeProgress(
                challenge.criteria,
                challenge.startsAt,
                challenge.endsAt,
                myHabit?.records.orEmpty(),
                today,
            ),
            theirProgress = computeChallengeProgress(
                challenge.criteria,
                challenge.startsAt,
                challenge.endsAt,
                theirHabit?.records.orEmpty(),
                today,
            ),
            daysLeft = computeChallengeProgress.daysLeft(
                challenge.startsAt,
                challenge.endsAt,
                today,
            ),
            habitName = definedHabit.name,
            habitIcon = definedHabit.icon,
            habitColorHex = definedHabit.colorHex,
            myCompletedDates = completedDatesInChallenge(
                myHabit?.records.orEmpty(),
                challenge.startsAt,
                challenge.endsAt,
            ),
            theirCompletedDates = completedDatesInChallenge(
                theirHabit?.records.orEmpty(),
                challenge.startsAt,
                challenge.endsAt,
            ),
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

    private sealed interface RenameChallengeOutcome {
        data class Success(val card: ChallengeCard) : RenameChallengeOutcome
        data object NotAllowed : RenameChallengeOutcome
        data class Failed(val error: AppError) : RenameChallengeOutcome
    }

    companion object {
        private const val TAG = "FriendsRepository"
        private const val STATUS_PENDING = "pending"
        private const val STATUS_ACCEPTED = "accepted"
        private const val STATUS_REJECTED = "rejected"
        private const val STATUS_ACTIVE = "active"
        private const val STATUS_DECLINED = "declined"
        private const val STATUS_CANCELLED = "cancelled"
        private const val OP_ACCEPTED_FRIENDS = "getAcceptedFriends"
        private const val OP_ACTIVE_CHALLENGES = "getActiveChallenges"
        private const val OP_CHALLENGE_HABIT_IDS = "challengeHabitIds"
        private const val OP_GET_CHALLENGE = "getChallenge"
        private const val OP_RESPOND_CHALLENGE = "respondToChallenge"
        private const val OP_CANCEL_CHALLENGE = "cancelChallenge"
        private const val OP_RENAME_CHALLENGE = "renameChallenge"
        private const val OP_PENDING_REQUESTS = "getPendingFriendRequests"
        private const val OP_SEARCH_PROFILES = "searchProfiles"
        private const val OP_SEND_REQUEST = "sendFriendRequest"
        private const val OP_RESPOND_REQUEST = "respondToFriendRequest"
        private const val OP_PUBLIC_HABITS = "getPublicHabitsForFriend"
        private const val OP_CREATE_CHALLENGE = "createChallenge"
    }
}
