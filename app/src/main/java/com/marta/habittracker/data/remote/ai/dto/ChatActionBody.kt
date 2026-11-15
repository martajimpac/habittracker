package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A proposed chat action. [type] is `create_habit` (name, description, icon, color, schedule)
 * or `set_completion` (habit_id, date, completed). Unused fields are null.
 */
@Serializable
internal data class ChatActionBody(
    val type: String,
    val name: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val schedule: HabitScheduleBody? = null,
    @SerialName("habit_id") val habitId: String? = null,
    val date: String? = null,
    val completed: Boolean? = null,
)
