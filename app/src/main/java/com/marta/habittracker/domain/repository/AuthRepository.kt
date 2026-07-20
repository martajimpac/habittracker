package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.models.AppError
import com.marta.habittracker.domain.models.User

interface AuthRepository {
    suspend fun doLogin(
        email: String,
        password: String
    ): DataResult<User, AppError>

    suspend fun doRegister(
        name: String,
        email: String,
        password: String
    ): DataResult<User, AppError>

    suspend fun isLoggedIn(): Boolean
}
