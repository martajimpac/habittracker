package com.aristidevs.habittracker.data.repository

import android.util.Log
import com.aristidevs.habittracker.data.datasource.api.ApiServices
import com.aristidevs.habittracker.data.response.toDomain
import com.aristidevs.habittracker.domain.entity.UserEntity
import com.aristidevs.habittracker.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(val api: ApiServices) : AuthRepository {

    override suspend fun doLogin(user: String, password: String): List<UserEntity> {
        val response = try {
            api.doLogin()
        } catch (e: Exception) {
            Log.i("DOLOGIN ERROR", "$e")
            listOf()
        }
        return response.map { it.toDomain() }
    }
}
