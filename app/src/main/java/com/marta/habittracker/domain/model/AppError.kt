package com.marta.habittracker.domain.model

sealed interface AppError {

    sealed interface Common : AppError {
        data object Network : Common
        data object Unauthorized : Common
        data object Unknown : Common
    }
}