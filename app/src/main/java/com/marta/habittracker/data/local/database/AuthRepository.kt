package com.marta.habittracker.data.local.database

import com.marta.habittracker.data.local.database.mappers.HabitMapper
import com.marta.habittracker.domain.repository.AuthRepository as DomainAuthRepository
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.coroutines.DispatchersProvider
import com.marta.habittracker.domain.models.AppError
import com.marta.habittracker.domain.models.User
import com.marta.habittracker.domain.repository.HabitRepository
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val habitMapper: HabitMapper,
    private val dispatchers: DispatchersProvider,
) : DomainAuthRepository {

    override suspend fun doLogin(email: String, password: String): DataResult<User, AppError> {
        TODO("Not yet implemented")
    }

}