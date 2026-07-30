package com.marta.habittracker.data.mapper

import com.marta.habittracker.core.toEpochMillis
import com.marta.habittracker.core.toInstant
import com.marta.habittracker.core.toJava
import com.marta.habittracker.core.toJavaList
import com.marta.habittracker.core.toKotlin
import com.marta.habittracker.core.toKotlinSet
import com.marta.habittracker.data.local.room.entities.HabitEntity
import com.marta.habittracker.data.local.room.entities.HabitRecordEntity
import com.marta.habittracker.data.local.room.entities.HabitWithRecordsEntity
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import java.util.UUID
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

    fun Habit.toEntity(
        updatedAt: Long = System.currentTimeMillis(),
        deletedAt: Long? = null,
    ): HabitEntity = HabitEntity(
        id = id,
        name = name,
        description = description,
        daysOfWeek = daysOfWeek.toJavaList(),
        icon = icon,
        colorHex = colorHex,
        reminderTime = reminderTime,
        createdAt = createdAt.toEpochMillis(),
        updatedAt = updatedAt,
        deletedAt = deletedAt,
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
        records = records.filter { it.deletedAt == null }.map { it.toDomain() },
    )

    fun map(habitWithRecordsEntity: HabitWithRecordsEntity): Habit = habitWithRecordsEntity.toDomain()

    fun HabitRecordEntity.toDomain(): HabitRecord = HabitRecord(
        id = id,
        habitId = habitId,
        date = date.toKotlin(),
        isCompleted = isCompleted,
    )

    fun HabitRecord.toEntity(
        id: String = this.id.ifBlank { UUID.randomUUID().toString() },
        updatedAt: Long = System.currentTimeMillis(),
        deletedAt: Long? = null,
    ): HabitRecordEntity = HabitRecordEntity(
        id = id,
        habitId = habitId,
        date = date.toJava(),
        isCompleted = isCompleted,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
