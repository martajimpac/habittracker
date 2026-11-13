package com.marta.habittracker.data.remote.supabase.datasources

import android.util.Log
import com.marta.habittracker.data.remote.supabase.ProfileRemote
import com.marta.habittracker.data.remote.supabase.SupabaseBuckets
import com.marta.habittracker.data.remote.supabase.SupabaseFunctions
import com.marta.habittracker.data.remote.supabase.SupabaseSession
import com.marta.habittracker.data.remote.supabase.SupabaseTables
import com.marta.habittracker.data.remote.supabase.helper.cacheBustAvatarUrl
import com.marta.habittracker.data.remote.supabase.dto.ProfileAvatarUrlUpdateDto
import com.marta.habittracker.data.remote.supabase.dto.ProfileDto
import io.github.jan.supabase.SupabaseClient
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
class ProfileRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
    private val session: SupabaseSession,
) : ProfileRemote {

    suspend fun fetchProfilesByIds(ids: Collection<String>): List<ProfileDto> {
        if (ids.isEmpty()) return emptyList()

        return try {
            Log.d(TAG, "fetchProfilesByIds started count=${ids.size}")
            val profiles = supabase.from(SupabaseTables.PROFILES)
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
        val userId = session.requireUserId()
        return try {
            Log.d(TAG, "updateMyProfile started")
            val updated = supabase.from(SupabaseTables.PROFILES)
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
        val userId = session.requireUserId()
        val path = avatarObjectPath(userId)
        return try {
            Log.d(TAG, "uploadAvatar started")
            val bucket = supabase.storage.from(SupabaseBuckets.AVATARS)
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
        val userId = session.requireUserId()
        val path = avatarObjectPath(userId)
        return try {
            Log.d(TAG, "clearAvatar started")
            try {
                supabase.storage.from(SupabaseBuckets.AVATARS).delete(path)
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
            supabase.functions.invoke(SupabaseFunctions.DELETE_ACCOUNT)
            Log.d(TAG, "deleteAccount finished")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "deleteAccount failed", e)
            throw e
        }
    }

    /**
     * Always writes avatar_url, including SQL null. Name-only updates must use
     * [updateMyProfile] which omits the column when avatarUrl is null.
     */
    private suspend fun updateAvatarUrl(url: String?): ProfileDto {
        val userId = session.requireUserId()
        return try {
            Log.d(TAG, "updateAvatarUrl started")
            val updated = supabase.from(SupabaseTables.PROFILES)
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
            val profiles = supabase.from(SupabaseTables.PROFILES)
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

    private companion object {
        const val TAG = "ProfileRemoteDS"
        const val PROFILE_SEARCH_LIMIT = 20L
        const val AVATAR_FILE_NAME = "avatar.jpg"

        fun avatarObjectPath(userId: String): String = "$userId/$AVATAR_FILE_NAME"
    }
}
