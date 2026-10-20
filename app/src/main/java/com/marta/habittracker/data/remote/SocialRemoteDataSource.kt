package com.marta.habittracker.data.remote

import android.util.Log
import com.marta.habittracker.core.IdGenerator
import com.marta.habittracker.data.remote.dto.ChallengeAcceptUpdateDto
import com.marta.habittracker.data.remote.dto.ChallengeDto
import com.marta.habittracker.data.remote.dto.ChallengeInsertDto
import com.marta.habittracker.data.remote.dto.ChallengeStatusUpdateDto
import com.marta.habittracker.data.remote.dto.FriendRequestDto
import com.marta.habittracker.data.remote.dto.FriendshipDto
import com.marta.habittracker.data.remote.dto.FriendshipStatusUpdateDto
import com.marta.habittracker.data.remote.dto.HabitDto
import com.marta.habittracker.data.remote.dto.HabitRecordDto
import com.marta.habittracker.data.remote.dto.ProfileAvatarUrlUpdateDto
import com.marta.habittracker.data.remote.dto.ProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class SocialRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) : ProfileRemote {

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

    override suspend fun fetchProfileById(id: String): ProfileDto? {
        return try {
            Log.d(TAG, "fetchProfileById started")
            val profile = fetchProfilesByIds(listOf(id)).firstOrNull()
            Log.d(TAG, "fetchProfileById finished found=${profile != null}")
            profile
        } catch (e: Exception) {
            Log.e(TAG, "fetchProfileById failed", e)
            throw e
        }
    }

    override suspend fun updateMyProfile(displayName: String?, avatarUrl: String?): ProfileDto {
        val userId = requireUserId()
        return try {
            Log.d(TAG, "updateMyProfile started")
            val updated = supabase.from(TABLE_PROFILES)
                .update({
                    if (displayName != null) {
                        set("display_name", displayName)
                    }
                    if (avatarUrl != null) {
                        set("avatar_url", avatarUrl)
                    }
                }) {
                    select()
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<ProfileDto>()
            Log.d(TAG, "updateMyProfile finished")
            updated
        } catch (e: Exception) {
            Log.e(TAG, "updateMyProfile failed", e)
            throw e
        }
    }

    override suspend fun uploadAvatar(jpegBytes: ByteArray): ProfileDto {
        val userId = requireUserId()
        val path = avatarObjectPath(userId)
        return try {
            Log.d(TAG, "uploadAvatar started")
            val bucket = supabase.storage.from(BUCKET_AVATARS)
            bucket.upload(path, jpegBytes) {
                upsert = true
                contentType = ContentType.Image.JPEG
            }
            val publicUrl = cacheBustAvatarUrl(
                publicUrl = bucket.publicUrl(path),
                versionMillis = Instant.now().toEpochMilli(),
            )
            val updated = updateAvatarUrl(publicUrl)
            Log.d(TAG, "uploadAvatar finished")
            updated
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "uploadAvatar failed", e)
            throw e
        }
    }

    override suspend fun clearAvatar(): ProfileDto {
        val userId = requireUserId()
        val path = avatarObjectPath(userId)
        return try {
            Log.d(TAG, "clearAvatar started")
            try {
                supabase.storage.from(BUCKET_AVATARS).delete(path)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "clearAvatar storage delete failed; still nulling avatar_url", e)
            }
            val updated = updateAvatarUrl(null)
            Log.d(TAG, "clearAvatar finished")
            updated
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "clearAvatar failed", e)
            throw e
        }
    }

    override suspend fun deleteAccount() {
        try {
            Log.d(TAG, "deleteAccount started")
            supabase.functions.invoke(FUNCTION_DELETE_ACCOUNT)
            Log.d(TAG, "deleteAccount finished")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "deleteAccount failed", e)
            throw e
        }
    }

    suspend fun registerDeviceToken(token: String) {
        try {
            Log.d(TAG, "registerDeviceToken started")
            supabase.functions.invoke(FUNCTION_REGISTER_DEVICE, body = mapOf("token" to token))
            Log.d(TAG, "registerDeviceToken finished")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "registerDeviceToken failed", e)
            throw e
        }
    }

    suspend fun deleteDeviceToken(token: String) {
        try {
            Log.d(TAG, "deleteDeviceToken started")
            supabase.from(TABLE_DEVICE_TOKENS).delete {
                filter {
                    eq("token", token)
                }
            }
            Log.d(TAG, "deleteDeviceToken finished")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "deleteDeviceToken failed", e)
            throw e
        }
    }

    /**
     * Always writes avatar_url, including SQL null. Name-only updates must use
     * [updateMyProfile] which omits the column when avatarUrl is null.
     */
    private suspend fun updateAvatarUrl(url: String?): ProfileDto {
        val userId = requireUserId()
        return try {
            Log.d(TAG, "updateAvatarUrl started")
            val updated = supabase.from(TABLE_PROFILES)
                .update(ProfileAvatarUrlUpdateDto(url)) {
                    select()
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<ProfileDto>()
            Log.d(TAG, "updateAvatarUrl finished")
            updated
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "updateAvatarUrl failed", e)
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

    suspend fun fetchChallengeById(id: String): ChallengeDto? {
        return try {
            Log.d(TAG, "fetchChallengeById started id=$id")
            val challenge = supabase.from(TABLE_CHALLENGES)
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
            val challenge = supabase.from(TABLE_CHALLENGES)
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
            val challenge = supabase.from(TABLE_CHALLENGES)
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
            val challenge = supabase.from(TABLE_CHALLENGES)
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
        private const val BUCKET_AVATARS = "avatars"
        private const val AVATAR_FILE_NAME = "avatar.jpg"
        private const val FUNCTION_DELETE_ACCOUNT = "delete-account"
        private const val FUNCTION_REGISTER_DEVICE = "register-device-token"
        private const val TABLE_DEVICE_TOKENS = "device_tokens"

        private fun avatarObjectPath(userId: String): String = "$userId/$AVATAR_FILE_NAME"
    }
}
