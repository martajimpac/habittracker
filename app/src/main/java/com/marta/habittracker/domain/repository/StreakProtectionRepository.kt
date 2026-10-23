package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.usecase.ProtectionReconcileResult
import java.time.LocalDate

interface StreakProtectionRepository {
    suspend fun reconcile(habits: List<Habit>, today: LocalDate): ProtectionReconcileResult
}
