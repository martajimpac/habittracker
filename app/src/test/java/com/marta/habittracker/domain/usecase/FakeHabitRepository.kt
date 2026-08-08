package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

class FakeHabitRepository(
    private val allHabitsWithRecords: List<Habit> = emptyList(),
) : HabitRepository {

    val insertedHabits = mutableListOf<Habit>()
    val toggledHabits = mutableListOf<Pair<Habit, LocalDate>>()
    var insertCalls: Int = 0
        private set
    var insertResult: DataResult<Unit, AppError> = DataResult.Success(Unit)
    var toggleResult: DataResult<Unit, AppError> = DataResult.Success(Unit)

    override fun getHabitsWithStatus(date: LocalDate): Flow<List<Habit>> = flowOf(emptyList())

    override fun getAllHabitsWithRecords(): Flow<List<Habit>> = flowOf(allHabitsWithRecords)

    override suspend fun toggleHabitCompletion(
        habit: Habit,
        date: LocalDate,
    ): DataResult<Unit, AppError> {
        toggledHabits.add(habit to date)
        return toggleResult
    }

    override fun getHabitById(id: String): Flow<Habit?> = flowOf(null)

    override fun getRecordsForHabit(habitId: String): Flow<List<HabitRecord>> = flowOf(emptyList())

    override suspend fun insertHabit(habit: Habit): DataResult<Unit, AppError> {
        insertCalls++
        if (insertResult is DataResult.Success) {
            insertedHabits.add(habit)
        }
        return insertResult
    }

    override suspend fun updateHabit(habit: Habit): DataResult<Unit, AppError> =
        DataResult.Success(Unit)

    override suspend fun deleteHabit(habit: Habit): DataResult<Unit, AppError> =
        DataResult.Success(Unit)

    override suspend fun syncFromRemote(): DataResult<Unit, AppError> = DataResult.Success(Unit)
}
