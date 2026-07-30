package com.marta.habittracker.domain.usecase

import com.marta.habittracker.core.toKotlinSet
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.repository.HabitRepository
import java.time.DayOfWeek
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class SaveHabit @Inject constructor(
    private val habitRepository: HabitRepository,
) {
    suspend operator fun invoke(
        name: String,
        description: String?,
        daysOfWeek: Set<DayOfWeek>,
        icon: String,
        colorHex: String,
        reminderTime: String?,
        id: String? = null,
    ): DataResult<String, AppError> {
        val habitId = id ?: UUID.randomUUID().toString()
        val result = habitRepository.insertHabit(
            Habit(
                id = habitId,
                name = name,
                description = description,
                daysOfWeek = daysOfWeek.toList().toKotlinSet(),
                icon = icon,
                colorHex = colorHex,
                reminderTime = reminderTime,
                createdAt = Instant.now(),
                records = emptyList(),
            ),
        )
        return when (result) {
            is DataResult.Success -> DataResult.Success(habitId)
            is DataResult.Error -> DataResult.Error(result.error)
        }
    }
}
