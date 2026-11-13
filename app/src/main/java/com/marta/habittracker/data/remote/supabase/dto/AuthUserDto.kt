package com.marta.habittracker.data.remote.supabase.dto

/** Signed-in Supabase Auth user, reduced to what the app reads. */
data class AuthUserDto(
    val id: String,
    val email: String?,
    val name: String,
    val emailConfirmed: Boolean,
)
