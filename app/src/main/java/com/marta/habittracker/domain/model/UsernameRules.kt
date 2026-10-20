package com.marta.habittracker.domain.model

object UsernameRules {
    private val stripped = Regex("[^a-z0-9_]")
    private val valid = Regex("^[a-z0-9_]{3,30}$")

    fun normalize(raw: String): String =
        raw.trim().lowercase().replace(stripped, "").take(30)

    fun isValid(value: String): Boolean = valid.matches(value)
}

data class UsernameAvailability(
    val normalized: String,
    val available: Boolean,
    val suggestions: List<String> = emptyList(),
)
