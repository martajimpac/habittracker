package com.marta.habittracker.domain.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getHabitsWithStatus(date: LocalDate): Flow<List<Habit>>
    fun getAllHabitsWithRecords(): Flow<List<Habit>>
    suspend fun toggleHabitCompletion(habit: Habit, date: LocalDate): DataResult<Unit, AppError>
    fun getHabitById(id: String): Flow<Habit?>
    fun getRecordsForHabit(habitId: String): Flow<List<HabitRecord>>
    suspend fun insertHabit(habit: Habit): DataResult<Unit, AppError>
    suspend fun updateHabit(habit: Habit): DataResult<Unit, AppError>
    suspend fun deleteHabit(habit: Habit): DataResult<Unit, AppError>
    suspend fun syncFromRemote(): DataResult<Unit, AppError>
}
