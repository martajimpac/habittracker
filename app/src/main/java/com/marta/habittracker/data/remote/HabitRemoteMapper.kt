package com.marta.habittracker.data.remote

import com.marta.habittracker.core.toJavaList
import com.marta.habittracker.data.local.room.entities.HabitEntity
import com.marta.habittracker.data.local.room.entities.HabitRecordEntity
import com.marta.habittracker.data.remote.dto.HabitDto
import com.marta.habittracker.data.remote.dto.HabitRecordDto
import com.marta.habittracker.domain.model.Habit
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object HabitRemoteMapper {

    fun Habit.toDto(
        userId: String,
        updatedAt: Instant = Instant.now(),
        deletedAt: Instant? = null,
    ): HabitDto =
        HabitDto(
            id = id,
            userId = userId,
            name = name,
            description = description,
            daysOfWeek = daysOfWeek.toJavaList().map { it.value }.sorted(),
            icon = icon,
            colorHex = colorHex,
            reminderTime = reminderTime,
            isPublic = isPublic,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString(),
            deletedAt = deletedAt?.toString(),
        )

    fun HabitDto.toEntity(): HabitEntity =
        HabitEntity(
            id = id,
            name = name,
            description = description,
            daysOfWeek = daysOfWeek.mapNotNull { isoDay ->
                runCatching { DayOfWeek.of(isoDay) }.getOrNull()
            },
            icon = icon,
            colorHex = colorHex,
            reminderTime = reminderTime,
            isPublic = isPublic,
            createdAt = createdAt.toEpochMillisOrNow(),
            updatedAt = updatedAt.toEpochMillisOrNow(),
            deletedAt = deletedAt.toEpochMillisOrNull(),
        )

    fun HabitRecordDto.toEntity(): HabitRecordEntity =
        HabitRecordEntity(
            id = id,
            habitId = habitId,
            date = LocalDate.parse(date),
            isCompleted = isCompleted,
            updatedAt = updatedAt.toEpochMillisOrNow(),
            deletedAt = deletedAt.toEpochMillisOrNull(),
        )

    fun HabitRecordEntity.toDto(userId: String): HabitRecordDto =
        HabitRecordDto(
            id = id,
            habitId = habitId,
            userId = userId,
            date = date.toString(),
            isCompleted = isCompleted,
            updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
            deletedAt = deletedAt?.let { Instant.ofEpochMilli(it).toString() },
        )

    private fun String?.toEpochMillisOrNow(): Long =
        this?.let { parseInstantMillis(it) } ?: System.currentTimeMillis()

    private fun String?.toEpochMillisOrNull(): Long? =
        this?.let { parseInstantMillis(it) }

    private fun parseInstantMillis(value: String): Long =
        runCatching { Instant.parse(value).toEpochMilli() }
            .recoverCatching {
                OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toInstant()
                    .toEpochMilli()
            }
            .getOrElse { System.currentTimeMillis() }
}
