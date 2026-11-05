package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.errors.AppError
import com.marta.habittracker.domain.model.UsernameAvailability

interface UsernameAvailabilityRepository {
    suspend fun check(raw: String): DataResult<UsernameAvailability, AppError>
}
