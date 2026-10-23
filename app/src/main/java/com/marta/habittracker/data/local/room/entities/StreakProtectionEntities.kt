package com.marta.habittracker.data.local.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "streak_protection_events",
    indices = [
        Index(value = ["habitId", "occasionKey", "type"], unique = true),
    ],
)
data class StreakProtectionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val occasionKey: String,
    val type: String,
)

@Entity(tableName = "streak_protection_baselines")
data class StreakBaselineEntity(
    @PrimaryKey val habitId: String,
    val baselineStreak: Int,
    val baselineProgress: Int,
    val since: String,
    val scheduleKey: String,
    val currentStreak: Int,
    val currentProgress: Int,
)
