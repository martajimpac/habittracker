package com.marta.habittracker.domain.models

sealed interface LoginError : AppError {

    data object InvalidCredentials : LoginError

    data object EmailNotVerified : LoginError
}