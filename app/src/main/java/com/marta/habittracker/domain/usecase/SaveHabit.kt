package com.marta.habittracker.domain.usecase

import com.marta.habittracker.core.IdGenerator
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.repository.HabitRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.datetime.DayOfWeek

class SaveHabit @Inject constructor(
    private val habitRepository: HabitRepository,
) {
    suspend operator fun invoke(
        name: String,
        description: String?,
        daysPerWeek: Int,
        scheduleMode: String = "daily",
        daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        icon: String,
        colorHex: String,
        reminderTime: String?,
        reminderEnabled: Boolean = false,
        isPublic: Boolean = false,
        id: String? = null,
    ): DataResult<String, AppError> {
        val habitId = id ?: IdGenerator.newId()
        val result = habitRepository.insertHabit(
            Habit(
                id = habitId,
                name = name,
                description = description,
                daysOfWeek = if (scheduleMode == "weekdays") daysOfWeek else DayOfWeek.entries.toSet(),
                icon = icon,
                colorHex = colorHex,
                reminderTime = reminderTime,
                reminderEnabled = reminderEnabled,
                isPublic = isPublic,
                createdAt = Instant.now(),
                records = emptyList(),
                daysPerWeek = daysPerWeek.coerceIn(1, 7),
                scheduleMode = scheduleMode,
            ),
        )
        return when (result) {
            is DataResult.Success -> DataResult.Success(habitId)
            is DataResult.Error -> DataResult.Error(result.error)
        }
    }
}
