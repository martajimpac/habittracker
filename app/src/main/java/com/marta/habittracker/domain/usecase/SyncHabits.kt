package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.repository.HabitRepository
import javax.inject.Inject

class SyncHabits @Inject constructor(
    private val habitRepository: HabitRepository,
) {
    suspend operator fun invoke(): DataResult<Unit, AppError> = habitRepository.syncFromRemote()
}
