package com.marta.habittracker.data.local.database

import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.data.local.database.entities.HabitRecordEntity
import com.marta.habittracker.data.local.database.mappers.HabitMapper
import com.marta.habittracker.domain.coroutines.DispatchersProvider
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.repository.HabitRepository as DomainHabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val habitMapper: HabitMapper,
    private val dispatchers: DispatchersProvider,
) : DomainHabitRepository {

    override fun getHabitsWithStatus(date: LocalDate): Flow<List<Habit>> {
        val dayOfWeek = date.dayOfWeek
        return habitDao.getHabitsWithRecords()
            .map { habitsList ->
                habitsList
                    .filter { it.habit.daysOfWeek.contains(dayOfWeek) }
                    .map { habitWithRecords ->
                        val habit = habitMapper.map(habitWithRecords)
                        val isCompleted = habitWithRecords.records.any { it.date == date && it.isCompleted }
                        habit.copy(isCompleted = isCompleted)
                    }
            }
    }

    override fun getAllHabitsWithRecords(): Flow<List<Habit>> =
        habitDao.getHabitsWithRecords().map { habitsList ->
            habitsList.map(habitMapper::map)
        }

    override suspend fun toggleHabitCompletion(habit: Habit, date: LocalDate) {
        val habitId = habit.id

        if (habit.isCompleted) {
            habitDao.deleteHabitRecord(habitId, date)
        } else {
            val record = HabitRecordEntity(
                habitId = habitId,
                date = date,
                isCompleted = true,
            )
            habitDao.upsertHabitRecord(record)
        }
    }

    override fun getHabitById(id: String): Flow<HabitEntity?> = habitDao.getHabitById(id)

    override fun getRecordsForHabit(habitId: String): Flow<List<HabitRecordEntity>> =
        habitDao.getRecordsForHabit(habitId)

    override suspend fun insertHabit(habit: HabitEntity) = habitDao.insertHabit(habit)

    override suspend fun updateHabit(habit: HabitEntity) = habitDao.updateHabit(habit)

    override suspend fun deleteHabit(habit: HabitEntity) = habitDao.deleteHabit(habit)
}
