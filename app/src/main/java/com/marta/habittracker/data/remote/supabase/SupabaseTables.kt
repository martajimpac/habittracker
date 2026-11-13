package com.marta.habittracker.data.remote.supabase

/** Postgrest table, Storage bucket, Edge Function and RPC names used by the Supabase data sources. */
internal object SupabaseTables {
    const val PROFILES = "profiles"
    const val FRIENDSHIPS = "friendships"
    const val CHALLENGES = "challenges"
    const val HABITS = "habits"
    const val HABIT_RECORDS = "habit_records"
    const val DEVICE_TOKENS = "device_tokens"
    const val STREAK_PROTECTION_EVENTS = "streak_protection_events"
}

internal object SupabaseBuckets {
    const val AVATARS = "avatars"
}

internal object SupabaseFunctions {
    const val DELETE_ACCOUNT = "delete-account"
    const val REGISTER_DEVICE = "register-device-token"
}

internal object SupabaseRpc {
    const val USERNAME_AVAILABILITY = "username_availability"
}
