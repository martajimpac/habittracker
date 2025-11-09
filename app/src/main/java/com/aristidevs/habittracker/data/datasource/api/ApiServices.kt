package com.aristidevs.habittracker.data.datasource.api

import com.aristidevs.habittracker.data.response.UserResponse
import retrofit2.http.GET

interface ApiServices {

    @GET("doLogin/.json")
    suspend fun doLogin():List<UserResponse>

}