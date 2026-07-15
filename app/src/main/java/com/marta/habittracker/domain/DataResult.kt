package com.marta.habittracker.domain

import com.marta.habittracker.domain.model.AppError

sealed interface DataResult<out T, out E : AppError> {
    data class Success<T>(val data: T) : DataResult<T, Nothing>
    data class Error<E : AppError>(val error: E) : DataResult<Nothing, E>
}