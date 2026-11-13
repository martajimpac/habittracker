package com.marta.habittracker.data.remote.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the signed-in Supabase user for the remote data sources and the repositories
 * that pass the user id into them. Throws when nobody is signed in.
 */
@Singleton
class SupabaseSession @Inject constructor(
    private val supabase: SupabaseClient,
) {

    fun requireUserId(): String =
        supabase.auth.currentUserOrNull()?.id
            ?: error("No authenticated Supabase user")

    /** Like [requireUserId], but waits for the stored session to load first (cold start). */
    suspend fun awaitUserId(): String {
        supabase.auth.awaitInitialization()
        return requireUserId()
    }
}
