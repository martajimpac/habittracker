package com.marta.habittracker.domain

object PasswordValidator {

    fun isValid(password: String?): Boolean {
        if (password == null) return false
        
        val hasEnoughLength = password.length >= 6
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        
        return hasEnoughLength && hasUppercase && hasLowercase && hasDigit
    }
}
