package com.marta.habittracker.domain.model.errors

sealed interface ProfileError : AppError {
    data object InvalidDisplayName : ProfileError
}
