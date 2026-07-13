package com.marta.habittracker.domain.repository

import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.data.local.database.entities.HabitRecordEntity
import com.marta.habittracker.domain.models.HabitWithStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getHabitsWithStatus(date: LocalDate): Flow<List<HabitWithStatus>>
    suspend fun toggleHabitCompletion(habitWithStatus: HabitWithStatus, date: LocalDate)
    fun getHabitById(id: Long): Flow<HabitEntity?>
    fun getRecordsForHabit(habitId: Long): Flow<List<HabitRecordEntity>>
    suspend fun insertHabit(habit: HabitEntity): Long
    suspend fun updateHabit(habit: HabitEntity)
    suspend fun deleteHabit(habit: HabitEntity)
}
