package com.marta.habittracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HabitDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val description: String? = null,
    @SerialName("days_of_week") val daysOfWeek: List<Int>,
    val icon: String,
    @SerialName("color_hex") val colorHex: String,
    @SerialName("reminder_time") val reminderTime: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class HabitRecordDto(
    val id: String,
    @SerialName("habit_id") val habitId: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    @SerialName("is_completed") val isCompleted: Boolean,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)
