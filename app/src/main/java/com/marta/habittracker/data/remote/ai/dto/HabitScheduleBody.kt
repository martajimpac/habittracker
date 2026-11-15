package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Habit frequency: [mode] is `daily`, `weekdays` (with [days]) or `weekly_count` (with [weeklyTarget]). */
@Serializable
internal data class HabitScheduleBody(
    val mode: String,
    val days: List<String> = emptyList(),
    @SerialName("weekly_target") val weeklyTarget: Int? = null,
)
