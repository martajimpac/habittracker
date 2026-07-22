package com.marta.habittracker.data.local.database.mappers

import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.data.local.database.entities.HabitRecordEntity
import com.marta.habittracker.data.local.database.entities.HabitWithRecordsEntity
import com.marta.habittracker.data.toEpochMillis
import com.marta.habittracker.data.toInstant
import com.marta.habittracker.data.toJava
import com.marta.habittracker.data.toJavaList
import com.marta.habittracker.data.toKotlin
import com.marta.habittracker.data.toKotlinSet
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import javax.inject.Inject

class HabitMapper @Inject constructor() {

    fun HabitEntity.toDomain(): Habit = Habit(
        id = id,
        name = name,
        description = description,
        daysOfWeek = daysOfWeek.toKotlinSet(),
        icon = icon,
        colorHex = colorHex,
        reminderTime = reminderTime,
        createdAt = createdAt.toInstant(),
        records = emptyList(),
    )

    fun Habit.toEntity(): HabitEntity = HabitEntity(
        id = id,
        name = name,
        description = description,
        daysOfWeek = daysOfWeek.toJavaList(),
        icon = icon,
        colorHex = colorHex,
        reminderTime = reminderTime,
        createdAt = createdAt.toEpochMillis(),
    )

    fun HabitWithRecordsEntity.toDomain(): Habit = Habit(
        id = habit.id,
        name = habit.name,
        description = habit.description,
        daysOfWeek = habit.daysOfWeek.toKotlinSet(),
        icon = habit.icon,
        colorHex = habit.colorHex,
        reminderTime = habit.reminderTime,
        createdAt = habit.createdAt.toInstant(),
        records = records.map { it.toDomain() },
    )

    fun map(habitWithRecordsEntity: HabitWithRecordsEntity): Habit = habitWithRecordsEntity.toDomain()

    fun HabitRecordEntity.toDomain(): HabitRecord = HabitRecord(
        habitId = habitId,
        date = date.toKotlin(),
        isCompleted = isCompleted,
    )

    fun HabitRecord.toEntity(): HabitRecordEntity = HabitRecordEntity(
        habitId = habitId,
        date = date.toJava(),
        isCompleted = isCompleted,
    )
}
