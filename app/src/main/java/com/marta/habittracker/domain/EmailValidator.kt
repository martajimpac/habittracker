package com.marta.habittracker.domain

object EmailValidator {

    private val emailRegex = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    fun isValid(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return emailRegex.matches(email.trim())
    }
}
