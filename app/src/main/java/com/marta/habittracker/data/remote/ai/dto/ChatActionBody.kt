package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A proposed chat action. [type] is `create_habit` (name, description, icon, color, schedule),
 * `set_completion` (habit_id, date, completed) or `create_challenge` (friend_name, name, icon,
 * color, schedule, criteria, duration_days). Unused fields are null.
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
    /** The friend's name as the person wrote it; never data of the resolved friend. */
    @SerialName("friend_name") val friendName: String? = null,
    val criteria: String? = null,
    @SerialName("duration_days") val durationDays: Int? = null,
)
