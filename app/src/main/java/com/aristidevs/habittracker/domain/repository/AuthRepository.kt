package com.aristidevs.habittracker.domain.repository

import com.aristidevs.habittracker.domain.entity.UserEntity

interface AuthRepository {
    suspend fun doLogin(user:String, password:String):List<UserEntity>
}