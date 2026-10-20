package com.marta.habittracker.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object Onboarding

@Serializable
object Login

@Serializable
enum class LoginMessage {
    PasswordResetRequestSent,
    PasswordUpdated,
    ResetLinkInvalid,
    AccountCreated,
    EmailConfirmed,
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
