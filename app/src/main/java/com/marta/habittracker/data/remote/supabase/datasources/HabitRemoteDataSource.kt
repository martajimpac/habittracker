package com.marta.habittracker.data.remote.supabase.datasources

import android.util.Log
import com.marta.habittracker.data.remote.supabase.SupabaseSession
import com.marta.habittracker.data.remote.supabase.SupabaseTables
import com.marta.habittracker.data.remote.supabase.dto.HabitDto
import com.marta.habittracker.data.remote.supabase.dto.HabitRecordDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
    private val session: SupabaseSession,
) {

    suspend fun requireUserId(): String = session.awaitUserId()

    suspend fun upsertHabit(habit: HabitDto) {
        try {
            supabase.from(SupabaseTables.HABITS).upsert(habit)
        } catch (e: Exception) {
            Log.e(TAG, "upsertHabit failed id=${habit.id}", e)
            throw e
        }
    }

    suspend fun upsertRecord(record: HabitRecordDto) {
        try {
            supabase.from(SupabaseTables.HABIT_RECORDS).upsert(record)
        } catch (e: Exception) {
            Log.e(TAG, "upsertRecord failed id=${record.id}", e)
            throw e
        }
    }

    suspend fun fetchHabits(): List<HabitDto> {
        return try {
            val userId = requireUserId()
            supabase.from(SupabaseTables.HABITS)
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<HabitDto>()
        } catch (e: Exception) {
            Log.e(TAG, "fetchHabits failed", e)
            throw e
        }
    }

    suspend fun fetchRecords(): List<HabitRecordDto> {
        return try {
            val userId = requireUserId()
            supabase.from(SupabaseTables.HABIT_RECORDS)
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<HabitRecordDto>()
        } catch (e: Exception) {
            Log.e(TAG, "fetchRecords failed", e)
            throw e
        }
    }

    /** Another user's public, non-deleted habits (friends list, challenge picker). */
    suspend fun fetchPublicHabitsForUser(userId: String): List<HabitDto> {
        return try {
            Log.d(TAG, "fetchPublicHabitsForUser started")
            val habits = supabase.from(SupabaseTables.HABITS)
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

    /** Non-deleted habits by id, whoever owns them (challenge participants). */
    suspend fun fetchHabitsByIds(ids: Collection<String>): List<HabitDto> {
        if (ids.isEmpty()) return emptyList()

        return try {
            Log.d(TAG, "fetchHabitsByIds started count=${ids.size}")
            val habits = supabase.from(SupabaseTables.HABITS)
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

    /** Non-deleted records for the given habits, whoever owns them. */
    suspend fun fetchRecordsForHabits(habitIds: Collection<String>): List<HabitRecordDto> {
        if (habitIds.isEmpty()) return emptyList()

        return try {
            Log.d(TAG, "fetchRecordsForHabits started count=${habitIds.size}")
            val records = supabase.from(SupabaseTables.HABIT_RECORDS)
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

    private companion object {
        const val TAG = "HabitRemoteDS"
    }
}
