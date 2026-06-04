package com.aristidevs.habittracker.data.network.api

import com.aristidevs.habittracker.data.network.response.UserResponse
import retrofit2.http.GET

interface ApiServices {

    @GET("doLogin/.json")
    suspend fun doLogin():List<UserResponse>

}