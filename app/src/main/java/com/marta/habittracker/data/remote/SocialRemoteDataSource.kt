package com.marta.habittracker.data.remote

import android.util.Log
import com.marta.habittracker.data.remote.dto.ChallengeDto
import com.marta.habittracker.data.remote.dto.ChallengeInsertDto
import com.marta.habittracker.data.remote.dto.FriendRequestDto
import com.marta.habittracker.data.remote.dto.FriendshipDto
import com.marta.habittracker.data.remote.dto.FriendshipStatusUpdateDto
import com.marta.habittracker.data.remote.dto.HabitDto
import com.marta.habittracker.data.remote.dto.HabitRecordDto
import com.marta.habittracker.data.remote.dto.ProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {

    fun requireUserId(): String =
        supabase.auth.currentUserOrNull()?.id
            ?: error("No authenticated user for social operation")

    suspend fun fetchProfilesByIds(ids: Collection<String>): List<ProfileDto> {
        if (ids.isEmpty()) return emptyList()

        return try {
            Log.d(TAG, "fetchProfilesByIds started count=${ids.size}")
            val profiles = supabase.from(TABLE_PROFILES)
                .select(Columns.ALL) {
                    filter {
                        isIn("id", ids.toList())
                    }
                }
                .decodeList<ProfileDto>()
            Log.d(TAG, "fetchProfilesByIds finished count=${profiles.size}")
            profiles
        } catch (e: Exception) {
            Log.e(TAG, "fetchProfilesByIds failed count=${ids.size}", e)
            throw e
        }
    }

    suspend fun searchProfilesByUsername(query: String): List<ProfileDto> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        return try {
            Log.d(TAG, "searchProfilesByUsername started")
            val profiles = supabase.from(TABLE_PROFILES)
                .select(Columns.ALL) {
                    filter {
                        ilike("username", "%$normalizedQuery%")
                    }
                    limit(PROFILE_SEARCH_LIMIT)
                }
                .decodeList<ProfileDto>()
            Log.d(TAG, "searchProfilesByUsername finished count=${profiles.size}")
            profiles
        } catch (e: Exception) {
            Log.e(TAG, "searchProfilesByUsername failed", e)
            throw e
        }
    }

    suspend fun fetchFriendshipsForUser(
        userId: String,
        status: String? = null,
    ): List<FriendshipDto> {
        return try {
            Log.d(TAG, "fetchFriendshipsForUser started status=$status")
            val friendships = supabase.from(TABLE_FRIENDSHIPS)
                .select(Columns.ALL) {
                    filter {
                        status?.let { eq("status", it) }
                        or {
                            eq("requester_id", userId)
                            eq("addressee_id", userId)
                        }
                    }
                }
                .decodeList<FriendshipDto>()
            Log.d(TAG, "fetchFriendshipsForUser finished count=${friendships.size}")
            friendships
        } catch (e: Exception) {
            Log.e(TAG, "fetchFriendshipsForUser failed status=$status", e)
            throw e
        }
    }

    suspend fun insertFriendRequest(
        requesterId: String,
        addresseeId: String,
    ): FriendshipDto {
        return try {
            Log.d(TAG, "insertFriendRequest started")
            val friendship = supabase.from(TABLE_FRIENDSHIPS)
                .insert(
                    FriendRequestDto(
                        id = UUID.randomUUID().toString(),
                        requesterId = requesterId,
                        addresseeId = addresseeId,
                    ),
                ) {
                    select()
                }
                .decodeSingle<FriendshipDto>()
            Log.d(TAG, "insertFriendRequest finished id=${friendship.id}")
            friendship
        } catch (e: Exception) {
            Log.e(TAG, "insertFriendRequest failed", e)
            throw e
        }
    }

    suspend fun updateFriendshipStatus(
        id: String,
        status: String,
    ): FriendshipDto {
        return try {
            Log.d(TAG, "updateFriendshipStatus started id=$id status=$status")
            val friendship = supabase.from(TABLE_FRIENDSHIPS)
                .update(FriendshipStatusUpdateDto(status)) {
                    select()
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<FriendshipDto>()
            Log.d(TAG, "updateFriendshipStatus finished id=$id")
            friendship
        } catch (e: Exception) {
            Log.e(TAG, "updateFriendshipStatus failed id=$id status=$status", e)
            throw e
        }
    }

    suspend fun fetchChallengesForUser(
        userId: String,
        statuses: Collection<String> = emptyList(),
    ): List<ChallengeDto> {
        return try {
            Log.d(TAG, "fetchChallengesForUser started statuses=${statuses.size}")
            val challenges = supabase.from(TABLE_CHALLENGES)
                .select(Columns.ALL) {
                    filter {
                        if (statuses.isNotEmpty()) {
                            isIn("status", statuses.toList())
                        }
                        or {
                            eq("challenger_id", userId)
                            eq("challenged_id", userId)
                        }
                    }
                }
                .decodeList<ChallengeDto>()
            Log.d(TAG, "fetchChallengesForUser finished count=${challenges.size}")
            challenges
        } catch (e: Exception) {
            Log.e(TAG, "fetchChallengesForUser failed statuses=${statuses.size}", e)
            throw e
        }
    }

    suspend fun insertChallenge(
        challengerId: String,
        challengedId: String,
        challengerHabitId: String,
        challengedHabitId: String,
        criteria: String,
        startsAt: Instant,
        endsAt: Instant,
    ): ChallengeDto {
        return try {
            Log.d(TAG, "insertChallenge started criteria=$criteria")
            val challenge = supabase.from(TABLE_CHALLENGES)
                .insert(
                    ChallengeInsertDto(
                        id = UUID.randomUUID().toString(),
                        challengerId = challengerId,
                        challengedId = challengedId,
                        challengerHabitId = challengerHabitId,
                        challengedHabitId = challengedHabitId,
                        criteria = criteria,
                        startsAt = startsAt.toString(),
                        endsAt = endsAt.toString(),
                    ),
                ) {
                    select()
                }
                .decodeSingle<ChallengeDto>()
            Log.d(TAG, "insertChallenge finished id=${challenge.id}")
            challenge
        } catch (e: Exception) {
            Log.e(TAG, "insertChallenge failed criteria=$criteria", e)
            throw e
        }
    }

    suspend fun fetchPublicHabitsForUser(userId: String): List<HabitDto> {
        return try {
            Log.d(TAG, "fetchPublicHabitsForUser started")
            val habits = supabase.from(TABLE_HABITS)
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                        eq("is_public", true)
                        exact("deleted_at", null)
                    }
                }
                .decodeList<HabitDto>()
            Log.d(TAG, "fetchPublicHabitsForUser finished count=${habits.size}")
            habits
        } catch (e: Exception) {
            Log.e(TAG, "fetchPublicHabitsForUser failed", e)
            throw e
        }
    }

    suspend fun fetchHabitsByIds(ids: Collection<String>): List<HabitDto> {
        if (ids.isEmpty()) return emptyList()

        return try {
            Log.d(TAG, "fetchHabitsByIds started count=${ids.size}")
            val habits = supabase.from(TABLE_HABITS)
                .select(Columns.ALL) {
                    filter {
                        isIn("id", ids.toList())
                        exact("deleted_at", null)
                    }
                }
                .decodeList<HabitDto>()
            Log.d(TAG, "fetchHabitsByIds finished count=${habits.size}")
            habits
        } catch (e: Exception) {
            Log.e(TAG, "fetchHabitsByIds failed count=${ids.size}", e)
            throw e
        }
    }

    suspend fun fetchRecordsForHabits(habitIds: Collection<String>): List<HabitRecordDto> {
        if (habitIds.isEmpty()) return emptyList()

        return try {
            Log.d(TAG, "fetchRecordsForHabits started count=${habitIds.size}")
            val records = supabase.from(TABLE_RECORDS)
                .select(Columns.ALL) {
                    filter {
                        isIn("habit_id", habitIds.toList())
                        exact("deleted_at", null)
                    }
                }
                .decodeList<HabitRecordDto>()
            Log.d(TAG, "fetchRecordsForHabits finished count=${records.size}")
            records
        } catch (e: Exception) {
            Log.e(TAG, "fetchRecordsForHabits failed count=${habitIds.size}", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "SocialRemoteDS"
        private const val TABLE_PROFILES = "profiles"
        private const val TABLE_FRIENDSHIPS = "friendships"
        private const val TABLE_CHALLENGES = "challenges"
        private const val TABLE_HABITS = "habits"
        private const val TABLE_RECORDS = "habit_records"
        private const val PROFILE_SEARCH_LIMIT = 20L
    }
}
