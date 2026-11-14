package com.marta.habittracker.data.remote.supabase

import android.util.Log
import com.marta.habittracker.data.remote.ai.BloomAccessTokens
import com.marta.habittracker.data.remote.ai.TokenRefresh
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Resolves the signed-in Supabase user for the remote data sources and the repositories
 * that pass the user id into them. Throws when nobody is signed in.
 * Also hands the session access token to the Bloom API client ([BloomAccessTokens]).
 */
@Singleton
class SupabaseSession @Inject constructor(
    private val supabase: SupabaseClient,
) : BloomAccessTokens {

    fun requireUserId(): String =
        supabase.auth.currentUserOrNull()?.id
            ?: error("No authenticated Supabase user")

    /** Like [requireUserId], but waits for the stored session to load first (cold start). */
    suspend fun awaitUserId(): String {
        supabase.auth.awaitInitialization()
        return requireUserId()
    }

    override suspend fun current(): String? {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentSessionOrNull()?.accessToken
    }

    override suspend fun refresh(): TokenRefresh {
        supabase.auth.awaitInitialization()
        if (supabase.auth.currentSessionOrNull() == null) return TokenRefresh.SignedOut
        return try {
            supabase.auth.refreshCurrentSession()
            supabase.auth.currentSessionOrNull()?.accessToken
                ?.let { TokenRefresh.Refreshed(it) }
                ?: TokenRefresh.SignedOut
        } catch (error: CancellationException) {
            throw error
        } catch (error: RestException) {
            // Supabase answered and rejected the refresh token (revoked or expired session).
            Log.e(TAG, "Session refresh rejected", error)
            TokenRefresh.SignedOut
        } catch (error: Exception) {
            Log.e(TAG, "Session refresh failed", error)
            TokenRefresh.Failed
        }
    }

    private companion object {
        const val TAG = "SupabaseSession"
    }
}
