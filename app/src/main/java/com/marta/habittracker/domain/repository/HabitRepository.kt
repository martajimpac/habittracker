package com.marta.habittracker.domain.repository

import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.data.local.database.entities.HabitRecordEntity
import com.marta.habittracker.domain.model.Habit
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getHabitsWithStatus(date: LocalDate): Flow<List<Habit>>
    suspend fun toggleHabitCompletion(habit: Habit, date: LocalDate)
    fun getHabitById(id: Long): Flow<HabitEntity?>
    fun getRecordsForHabit(habitId: Long): Flow<List<HabitRecordEntity>>
    suspend fun insertHabit(habit: HabitEntity): Long
    suspend fun updateHabit(habit: HabitEntity)
    suspend fun deleteHabit(habit: HabitEntity)
}
