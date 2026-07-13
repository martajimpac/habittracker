package com.marta.habittracker.data.local.database

import com.marta.habittracker.domain.repository.HabitRepository as DomainHabitRepository
import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.data.local.database.entities.HabitRecordEntity
import com.marta.habittracker.data.local.database.mappers.HabitMapper
import com.marta.habittracker.domain.coroutines.DispatchersProvider
import com.marta.habittracker.domain.models.HabitWithStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val habitMapper: HabitMapper,
    private val dispatchers: DispatchersProvider,
) : DomainHabitRepository {

    override fun getHabitsWithStatus(date: LocalDate): Flow<List<HabitWithStatus>> {
        val dayOfWeek = DayOfWeek.of(date.dayOfWeek.value)
        return habitDao.getHabitsWithRecords()
            .map { habitsList ->
                habitsList
                    .filter {
                        it.habit.daysOfWeek.contains(dayOfWeek) }
                    .map { habitWithRecords ->
                        val habit = habitMapper.map(habitWithRecords)
                        val isCompleted = habitWithRecords.records.any { it.date == date && it.isCompleted }
                        HabitWithStatus(
                            id = habit.id,
                            name = habit.name,
                            description = habit.description,
                            daysOfWeek = habit.daysOfWeek,
                            createdAt = habit.createdAt,
                            records = habit.records,
                            isCompleted = isCompleted
                        )
                    }
            }


        /*.onStart {
            refreshScope.launch {
                if (!refreshMutex.tryLock()) return@launch
                try {
                    refreshProduct()
                } catch (e: Exception) {
                } finally {
                    refreshMutex.unlock()
                }
            }
        }.catch {
            // Log importante
        }*/
    }

    override suspend fun toggleHabitCompletion(habitWithStatus: HabitWithStatus, date: LocalDate) {
        val habitId = habitWithStatus.id

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

    override suspend fun insertHabit(habit: HabitEntity): Long = habitDao.insertHabit(habit)

    override suspend fun updateHabit(habit: HabitEntity) = habitDao.updateHabit(habit)

    override suspend fun deleteHabit(habit: HabitEntity) = habitDao.deleteHabit(habit)
}
