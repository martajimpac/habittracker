package com.marta.habittracker.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object Onboarding

@Serializable
data class Login(
    val message: LoginMessage? = null,
)

@Serializable
enum class LoginMessage {
    PasswordResetRequestSent,
    PasswordUpdated,
    ResetLinkInvalid,
}

@Serializable
object Register

@Serializable
data class ForgotPassword(
    val email: String = "",
)

@Serializable
object ResetPassword

@Serializable
object Home
