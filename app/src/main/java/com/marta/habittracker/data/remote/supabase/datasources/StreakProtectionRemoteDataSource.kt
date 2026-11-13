package com.marta.habittracker.data.remote.supabase.datasources

import android.util.Log
import com.marta.habittracker.data.remote.supabase.SupabaseSession
import com.marta.habittracker.data.remote.supabase.SupabaseTables
import com.marta.habittracker.data.remote.supabase.dto.StreakProtectionEventDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakProtectionRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
    private val session: SupabaseSession,
) {
    private fun requireUserId(): String = session.requireUserId()

    suspend fun fetchEvents(): List<StreakProtectionEventDto> {
        return try {
            val userId = requireUserId()
            supabase.from(SupabaseTables.STREAK_PROTECTION_EVENTS)
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
            supabase.from(SupabaseTables.STREAK_PROTECTION_EVENTS).insert(event.copy(userId = requireUserId()))
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

    private companion object {
        const val TAG = "StreakProtectRemote"
    }
}
