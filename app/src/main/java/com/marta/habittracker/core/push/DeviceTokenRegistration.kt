package com.marta.habittracker.core.push

interface DeviceTokenRegistration {
    suspend fun registerCurrentToken()
    suspend fun clearCurrentToken()
}
