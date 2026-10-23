package com.marta.habittracker.data.remote

import android.util.Log
import com.marta.habittracker.data.remote.dto.StreakProtectionEventDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakProtectionRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    fun requireUserId(): String =
        supabase.auth.currentUserOrNull()?.id
            ?: error("No authenticated user for streak protections")

    suspend fun fetchEvents(): List<StreakProtectionEventDto> {
        return try {
            val userId = requireUserId()
            supabase.from(TABLE)
                .select(Columns.ALL) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<StreakProtectionEventDto>()
        } catch (e: Exception) {
            Log.e(TAG, "fetchEvents failed", e)
            throw e
        }
    }

    suspend fun insertEvent(event: StreakProtectionEventDto) {
        try {
            supabase.from(TABLE).insert(event.copy(userId = requireUserId()))
            Log.d(TAG, "insertEvent ok habitId=${event.habitId} key=${event.occasionKey}")
        } catch (e: Exception) {
            val duplicate = e.message?.contains("duplicate", ignoreCase = true) == true ||
                e.message?.contains("23505") == true
            if (duplicate) {
                Log.d(TAG, "insertEvent already stored habitId=${event.habitId} key=${event.occasionKey}")
            } else {
                Log.e(TAG, "insertEvent failed habitId=${event.habitId}", e)
                throw e
            }
        }
    }

    companion object {
        private const val TAG = "StreakProtectRemote"
        private const val TABLE = "streak_protection_events"
    }
}
