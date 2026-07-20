package com.marta.habittracker.domain.models

sealed interface RegisterError : AppError {
    data object EmailAlreadyRegistered : RegisterError
    data object WeakPassword : RegisterError
}
