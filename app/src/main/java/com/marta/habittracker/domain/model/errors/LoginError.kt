package com.marta.habittracker.domain.model.errors

sealed interface LoginError : AppError {

    data object InvalidCredentials : LoginError

    data object EmailNotVerified : LoginError
}