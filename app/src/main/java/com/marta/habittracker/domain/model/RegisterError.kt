package com.marta.habittracker.domain.model

sealed interface RegisterError : AppError {
    data object InvalidEmail : RegisterError
    data object WeakPassword : RegisterError
    data object EmailAlreadyRegistered : RegisterError
    data object EmailConfirmationRequired : RegisterError
}
