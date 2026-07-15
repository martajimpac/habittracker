package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.User

interface AuthRepository {
    suspend fun doLogin(
        email: String,
        password: String
    ): DataResult<User, AppError>
}