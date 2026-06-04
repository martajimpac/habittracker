package com.aristidevs.habittracker.data.local.database

import com.aristidevs.habittracker.domain.repository.HabitRepository as DomainHabitRepository
import com.aristidevs.habittracker.data.local.database.entities.HabitEntity
import com.aristidevs.habittracker.data.local.database.entities.HabitRecordEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao
) : DomainHabitRepository {

    override fun getHabitsWithStatus(date: LocalDate): Flow<List<HabitWithStatus>> {
        val dayOfWeek = date.dayOfWeek.value
        return habitDao.getHabitsWithStatusForDay(date, dayOfWeek)
    }

    override suspend fun toggleHabitCompletion(habitWithStatus: HabitWithStatus, date: LocalDate) {
        val habitId = habitWithStatus.habit.id

        if (habitWithStatus.isCompleted) {
            habitDao.deleteHabitRecord(habitId, date)
        } else {
            val record = HabitRecordEntity(
                habitId = habitId,
                date = date,
                isCompleted = true
            )
            habitDao.upsertHabitRecord(record)
        }
    }

    override fun getHabitById(id: Long): Flow<HabitEntity?> = habitDao.getHabitById(id)

    override fun getRecordsForHabit(habitId: Long): Flow<List<HabitRecordEntity>> =
        habitDao.getRecordsForHabit(habitId)

    override suspend fun insertHabit(habit: HabitEntity) = habitDao.insertHabit(habit)

    override suspend fun updateHabit(habit: HabitEntity) = habitDao.updateHabit(habit)

    override suspend fun deleteHabit(habit: HabitEntity) = habitDao.deleteHabit(habit)
}
