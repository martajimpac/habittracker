package com.marta.habittracker.domain.usecase

import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.domain.repository.HabitRepository
import java.time.DayOfWeek
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
    ): String {
        val habitId = id ?: UUID.randomUUID().toString()
        habitRepository.insertHabit(
            HabitEntity(
                id = habitId,
                name = name,
                description = description,
                daysOfWeek = daysOfWeek.toList(),
                icon = icon,
                colorHex = colorHex,
                reminderTime = reminderTime,
            ),
        )
        return habitId
    }
}
