package com.marta.habittracker.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val daysOfWeek: List<DayOfWeek>,
    val icon: String = "water_drop",
    val colorHex: String = "#6750A4",
    val reminderTime: String? = null,
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
)
