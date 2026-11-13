package com.marta.habittracker.data.remote.supabase.datasources

import android.util.Log
import com.marta.habittracker.core.IdGenerator
import com.marta.habittracker.data.remote.supabase.SupabaseTables
import com.marta.habittracker.data.remote.supabase.dto.FriendRequestDto
import com.marta.habittracker.data.remote.supabase.dto.FriendshipDto
import com.marta.habittracker.data.remote.supabase.dto.FriendshipStatusUpdateDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendshipRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {

    suspend fun fetchFriendshipsForUser(
        userId: String,
        status: String? = null,
    ): List<FriendshipDto> {
        return try {
            Log.d(TAG, "fetchFriendshipsForUser started status=$status")
            val friendships = supabase.from(SupabaseTables.FRIENDSHIPS)
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
            val friendship = supabase.from(SupabaseTables.FRIENDSHIPS)
                .insert(
                    FriendRequestDto(
                        id = IdGenerator.newId(),
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
            val friendship = supabase.from(SupabaseTables.FRIENDSHIPS)
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

    private companion object {
        const val TAG = "FriendshipRemoteDS"
    }
}
