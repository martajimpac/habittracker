package com.marta.habittracker.data.local.database.entities

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
            onDelete = ForeignKey.CASCADE // Si borras el hábito, se borran sus registros
        )
    ],
    indices = [Index(value = ["habitId", "date"], unique = true)] // Evita duplicados para el mismo día
)
data class HabitRecordEntity(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0,
    val habitId: Long,
    val date: LocalDate,
    val isCompleted: Boolean
)