package com.marta.habittracker.domain.model

sealed interface ProfileError : AppError {
    data object InvalidDisplayName : ProfileError
}
