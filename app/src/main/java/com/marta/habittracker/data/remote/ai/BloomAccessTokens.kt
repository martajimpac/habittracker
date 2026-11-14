package com.marta.habittracker.data.remote.ai

/**
 * Supabase session token sent to the Bloom API as `Authorization: Bearer`.
 * Implementations must never log the token.
 */
interface BloomAccessTokens {

    /** Access token of the signed-in user, or null when nobody is signed in. */
    suspend fun current(): String?

    /** Asks Supabase for a fresh session after the Bloom API rejected the current token. */
    suspend fun refresh(): TokenRefresh
}

sealed interface TokenRefresh {
    data class Refreshed(val accessToken: String) : TokenRefresh

    /** No session, or Supabase rejected the refresh token: the user has to sign in again. */
    data object SignedOut : TokenRefresh

    /** The refresh could not reach Supabase (e.g. no network); the session may still be valid. */
    data object Failed : TokenRefresh
}
