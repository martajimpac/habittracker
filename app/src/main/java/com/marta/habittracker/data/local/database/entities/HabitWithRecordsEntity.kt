package com.marta.habittracker.data.local.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class HabitWithRecordsEntity(
    @Embedded val habit: HabitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val records: List<HabitRecordEntity>
)