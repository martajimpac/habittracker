package com.aristidevs.habittracker.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    // Guardaremos los días como una lista: [1, 3, 5] (Lunes, Miércoles, Viernes)
    val daysOfWeek: List<DayOfWeek>,
    val createdAt: Long = System.currentTimeMillis()
)