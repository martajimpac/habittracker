package com.marta.habittracker.domain.repository

import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.data.local.database.entities.HabitRecordEntity
import com.marta.habittracker.domain.model.Habit
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getHabitsWithStatus(date: LocalDate): Flow<List<Habit>>
    fun getAllHabitsWithRecords(): Flow<List<Habit>>
    suspend fun toggleHabitCompletion(habit: Habit, date: LocalDate)
    fun getHabitById(id: String): Flow<HabitEntity?>
    fun getRecordsForHabit(habitId: String): Flow<List<HabitRecordEntity>>
    suspend fun insertHabit(habit: HabitEntity)
    suspend fun updateHabit(habit: HabitEntity)
    suspend fun deleteHabit(habit: HabitEntity)
}
