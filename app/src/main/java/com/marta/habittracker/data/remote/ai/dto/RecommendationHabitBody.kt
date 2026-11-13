package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One habit's week stats in a Bloom chat request. */
@Serializable
internal data class RecommendationHabitBody(
    val name: String,
    @SerialName("completed_days") val completedDays: Int,
    @SerialName("total_days") val totalDays: Int,
)
