package com.marta.habittracker.data.remote

import android.util.Log
import com.marta.habittracker.data.remote.dto.HabitDto
import com.marta.habittracker.data.remote.dto.HabitRecordDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {

    fun requireUserId(): String =
        supabase.auth.currentUserOrNull()?.id
            ?: error("No authenticated user for habit sync")

    suspend fun upsertHabit(habit: HabitDto) {
        try {
            supabase.from(TABLE_HABITS).upsert(habit)
        } catch (e: Exception) {
            Log.e(TAG, "upsertHabit failed id=${habit.id}", e)
            throw e
        }
    }

    suspend fun upsertRecord(record: HabitRecordDto) {
        try {
            supabase.from(TABLE_RECORDS).upsert(record)
        } catch (e: Exception) {
            Log.e(TAG, "upsertRecord failed id=${record.id}", e)
            throw e
        }
    }

    suspend fun fetchHabits(): List<HabitDto> {
        return try {
            val userId = requireUserId()
            supabase.from(TABLE_HABITS)
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
            supabase.from(TABLE_RECORDS)
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

    companion object {
        private const val TAG = "HabitRemoteDS"
        private const val TABLE_HABITS = "habits"
        private const val TABLE_RECORDS = "habit_records"
    }
}
