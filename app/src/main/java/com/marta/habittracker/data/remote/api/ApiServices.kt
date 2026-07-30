package com.marta.habittracker.data.remote.api

import com.marta.habittracker.data.remote.response.UserResponse
import retrofit2.http.GET

interface ApiServices {

    @GET("doLogin/{user}/{password}")
    suspend fun doLogin(user: String, password: String):List<UserResponse>

}