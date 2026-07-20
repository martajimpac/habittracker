package com.marta.habittracker.data.local.database

import com.marta.habittracker.data.local.database.mappers.HabitMapper
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.coroutines.DispatchersProvider
import com.marta.habittracker.domain.models.AppError
import com.marta.habittracker.domain.models.User
import com.marta.habittracker.domain.repository.AuthRepository as DomainAuthRepository
import javax.inject.Inject

/**
 * Unused local stub. Production auth is [com.marta.habittracker.data.network.AuthRepositoryImpl].
 */
class AuthRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val habitMapper: HabitMapper,
    private val dispatchers: DispatchersProvider,
) : DomainAuthRepository {

    override suspend fun doLogin(email: String, password: String): DataResult<User, AppError> {
        TODO("Not yet implemented")
    }

    override suspend fun doRegister(
        name: String,
        email: String,
        password: String,
    ): DataResult<User, AppError> {
        TODO("Not yet implemented")
    }

    override suspend fun isLoggedIn(): Boolean {
        TODO("Not yet implemented")
    }
}
