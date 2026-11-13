package com.marta.habittracker.data.remote.supabase.datasources

import android.util.Log
import com.marta.habittracker.core.IdGenerator
import com.marta.habittracker.data.remote.supabase.SupabaseTables
import com.marta.habittracker.data.remote.supabase.dto.ChallengeAcceptUpdateDto
import com.marta.habittracker.data.remote.supabase.dto.ChallengeDto
import com.marta.habittracker.data.remote.supabase.dto.ChallengeInsertDto
import com.marta.habittracker.data.remote.supabase.dto.ChallengeStatusUpdateDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class ChallengeRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {

    suspend fun fetchChallengesForUser(
        userId: String,
        statuses: Collection<String> = emptyList(),
    ): List<ChallengeDto> {
        return try {
            Log.d(TAG, "fetchChallengesForUser started statuses=${statuses.size}")
            val challenges = supabase.from(SupabaseTables.CHALLENGES)
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

    suspend fun fetchChallengeById(id: String): ChallengeDto? {
        return try {
            Log.d(TAG, "fetchChallengeById started id=$id")
            val challenge = supabase.from(SupabaseTables.CHALLENGES)
                .select(Columns.ALL) {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingleOrNull<ChallengeDto>()
            Log.d(TAG, "fetchChallengeById finished id=$id found=${challenge != null}")
            challenge
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "fetchChallengeById failed id=$id", e)
            throw e
        }
    }

    suspend fun updateChallengeStatus(
        id: String,
        status: String,
    ): ChallengeDto {
        return try {
            Log.d(TAG, "updateChallengeStatus started id=$id status=$status")
            val challenge = supabase.from(SupabaseTables.CHALLENGES)
                .update(ChallengeStatusUpdateDto(status)) {
                    select()
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<ChallengeDto>()
            Log.d(TAG, "updateChallengeStatus finished id=$id")
            challenge
        } catch (e: Exception) {
            Log.e(TAG, "updateChallengeStatus failed id=$id status=$status", e)
            throw e
        }
    }

    suspend fun acceptChallenge(
        id: String,
        challengedHabitId: String,
    ): ChallengeDto {
        return try {
            Log.d(TAG, "acceptChallenge started id=$id")
            val challenge = supabase.from(SupabaseTables.CHALLENGES)
                .update(
                    ChallengeAcceptUpdateDto(
                        status = "active",
                        challengedHabitId = challengedHabitId,
                    ),
                ) {
                    select()
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<ChallengeDto>()
            Log.d(TAG, "acceptChallenge finished id=$id")
            challenge
        } catch (e: Exception) {
            Log.e(TAG, "acceptChallenge failed id=$id", e)
            throw e
        }
    }

    suspend fun insertChallenge(
        challengerId: String,
        challengedId: String,
        challengerHabitId: String,
        challengedHabitId: String?,
        criteria: String,
        startsAt: Instant,
        endsAt: Instant,
    ): ChallengeDto {
        return try {
            Log.d(TAG, "insertChallenge started criteria=$criteria")
            val challenge = supabase.from(SupabaseTables.CHALLENGES)
                .insert(
                    ChallengeInsertDto(
                        id = IdGenerator.newId(),
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

    private companion object {
        const val TAG = "ChallengeRemoteDS"
    }
}
