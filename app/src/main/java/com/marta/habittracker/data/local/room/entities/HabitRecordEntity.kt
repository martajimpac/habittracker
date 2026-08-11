package com.marta.habittracker.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "habit_records",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["habitId", "date"], unique = true)],
)
data class HabitRecordEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: LocalDate,
    val isCompleted: Boolean,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
)
