package com.marta.habittracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChallengeDto(
    val id: String,
    @SerialName("challenger_id") val challengerId: String,
    @SerialName("challenged_id") val challengedId: String,
    @SerialName("challenger_habit_id") val challengerHabitId: String,
    @SerialName("challenged_habit_id") val challengedHabitId: String,
    val criteria: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
internal data class ChallengeInsertDto(
    val id: String,
    @SerialName("challenger_id") val challengerId: String,
    @SerialName("challenged_id") val challengedId: String,
    @SerialName("challenger_habit_id") val challengerHabitId: String,
    @SerialName("challenged_habit_id") val challengedHabitId: String,
    val criteria: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val status: String = "pending",
)
