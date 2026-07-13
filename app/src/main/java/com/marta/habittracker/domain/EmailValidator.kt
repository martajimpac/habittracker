package com.marta.habittracker.domain

import android.util.Patterns

object EmailValidator {

    fun isValid(email: String?): Boolean {
        return !email.isNullOrBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }
}
