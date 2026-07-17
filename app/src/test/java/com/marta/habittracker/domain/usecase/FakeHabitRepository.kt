package com.marta.habittracker.domain.usecase

import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.data.local.database.entities.HabitRecordEntity
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

class FakeHabitRepository(
    private val insertResult: Long = 42L,
) : HabitRepository {

    val insertedHabits = mutableListOf<HabitEntity>()
    var insertCalls: Int = 0
        private set

    override fun getHabitsWithStatus(date: LocalDate): Flow<List<Habit>> = flowOf(emptyList())

    override suspend fun toggleHabitCompletion(habit: Habit, date: LocalDate) = Unit

    override fun getHabitById(id: Long): Flow<HabitEntity?> = flowOf(null)

    override fun getRecordsForHabit(habitId: Long): Flow<List<HabitRecordEntity>> = flowOf(emptyList())

    override suspend fun insertHabit(habit: HabitEntity): Long {
        insertCalls++
        insertedHabits.add(habit)
        return insertResult
    }

    override suspend fun updateHabit(habit: HabitEntity) = Unit

    override suspend fun deleteHabit(habit: HabitEntity) = Unit
}
