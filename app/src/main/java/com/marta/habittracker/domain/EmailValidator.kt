package com.marta.habittracker.domain

object EmailValidator {

    private val EMAIL_REGEX = Regex(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
                "@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

    fun isValid(email: String?): Boolean {
        return !email.isNullOrBlank() && EMAIL_REGEX.matches(email.trim())
    }
}
