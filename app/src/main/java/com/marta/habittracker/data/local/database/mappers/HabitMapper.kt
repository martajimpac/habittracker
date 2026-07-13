package com.marta.habittracker.data.local.database.mappers

import com.marta.habittracker.data.*
import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.data.local.database.entities.HabitRecordEntity
import com.marta.habittracker.data.local.database.entities.HabitWithRecordsEntity
import com.marta.habittracker.domain.models.HabitWithStatus
import com.marta.habittracker.domain.models.HabitRecord
import javax.inject.Inject

class HabitMapper @Inject constructor() {

    fun HabitEntity.toDomain(): HabitWithStatus = HabitWithStatus(
        id = id,
        name = name,
        description = description,
        daysOfWeek = daysOfWeek.toKotlinSet(),
        createdAt = createdAt.toInstant(),
        records = emptyList()
    )

    fun HabitWithStatus.toEntity(): HabitEntity = HabitEntity(
        id = id,
        name = name,
        description = description,
        daysOfWeek = daysOfWeek.toJavaList(),
        createdAt = createdAt.toEpochMillis()
    )

    fun HabitWithRecordsEntity.toDomain(): HabitWithStatus = HabitWithStatus(
        id = habit.id,
        name = habit.name,
        description = habit.description,
        daysOfWeek = habit.daysOfWeek.toKotlinSet(),
        createdAt = habit.createdAt.toInstant(),
        records = records.map { it.toDomain() }
    )

    fun map(habitWithRecordsEntity: HabitWithRecordsEntity): HabitWithStatus = habitWithRecordsEntity.toDomain()

    fun HabitRecordEntity.toDomain(): HabitRecord = HabitRecord(
        habitId = habitId,
        date = date.toKotlin(),
        isCompleted = isCompleted
    )

    fun HabitRecord.toEntity(): HabitRecordEntity = HabitRecordEntity(
        habitId = habitId,
        date = date.toJava(),
        isCompleted = isCompleted
    )
}
