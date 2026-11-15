package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One habit in a Bloom chat request: week counts, plus what Bloom needs to propose marking a day. */
@Serializable
internal data class RecommendationHabitBody(
    val name: String,
    @SerialName("completed_days") val completedDays: Int,
    @SerialName("total_days") val totalDays: Int,
    val id: String? = null,
    val schedule: HabitScheduleBody? = null,
    @SerialName("done_dates") val doneDates: List<String> = emptyList(),
)
