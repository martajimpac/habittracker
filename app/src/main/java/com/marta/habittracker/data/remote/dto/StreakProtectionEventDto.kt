package com.marta.habittracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreakProtectionEventDto(
    @SerialName("habit_id") val habitId: String,
    @SerialName("occasion_key") val occasionKey: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("user_id") val userId: String? = null,
)
