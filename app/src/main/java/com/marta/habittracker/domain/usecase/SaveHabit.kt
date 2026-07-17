package com.marta.habittracker.domain.usecase

import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.domain.repository.HabitRepository
import java.time.DayOfWeek
import javax.inject.Inject

class SaveHabit @Inject constructor(
    private val habitRepository: HabitRepository,
) {
    suspend operator fun invoke(
        name: String,
        description: String?,
        daysOfWeek: Set<DayOfWeek>,
    ): Long {
        return habitRepository.insertHabit(
            HabitEntity(
                name = name,
                description = description,
                daysOfWeek = daysOfWeek.toList(),
            ),
        )
    }
}
