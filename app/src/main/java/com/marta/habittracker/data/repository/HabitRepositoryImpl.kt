package com.marta.habittracker.data.repository

import android.util.Log
import com.marta.habittracker.core.network.NetworkChecker
import com.marta.habittracker.core.toEpochMillis
import com.marta.habittracker.data.local.room.HabitDao
import com.marta.habittracker.data.local.room.entities.HabitRecordEntity
import com.marta.habittracker.data.mapper.HabitMapper
import com.marta.habittracker.data.remote.HabitRemoteDataSource
import com.marta.habittracker.data.remote.HabitRemoteMapper
import com.marta.habittracker.data.remote.HabitRemoteMapper.toDto
import com.marta.habittracker.data.remote.HabitRemoteMapper.toEntity
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.repository.HabitRepository as DomainHabitRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val habitMapper: HabitMapper,
    private val remote: HabitRemoteDataSource,
    private val networkChecker: NetworkChecker,
) : DomainHabitRepository {

    override fun getHabitsWithStatus(date: LocalDate): Flow<List<Habit>> {
        val dayOfWeek = date.dayOfWeek
        return habitDao.getHabitsWithRecords()
            .map { habitsList ->
                habitsList
                    .filter { it.habit.daysOfWeek.contains(dayOfWeek) }
                    .map { habitWithRecords ->
                        val habit = habitMapper.map(habitWithRecords)
                        val isCompleted = habitWithRecords.records.any {
                            it.deletedAt == null && it.date == date && it.isCompleted
                        }
                        habit.copy(isCompleted = isCompleted)
                    }
            }
    }

    override fun getAllHabitsWithRecords(): Flow<List<Habit>> =
        habitDao.getHabitsWithRecords().map { habitsList ->
            habitsList.map(habitMapper::map)
        }

    override suspend fun toggleHabitCompletion(
        habit: Habit,
        date: LocalDate,
    ): DataResult<Unit, AppError> {
        if (!networkChecker.isOnline()) return DataResult.Error(AppError.Common.Network)

        val now = System.currentTimeMillis()
        val newCompleted = !habit.isCompleted
        val existing = habitDao.getRecordForHabitOnDate(habit.id, date)
        val recordEntity = existing?.copy(
            isCompleted = newCompleted,
            updatedAt = now,
            deletedAt = null,
        ) ?: HabitRecordEntity(
            id = UUID.randomUUID().toString(),
            habitId = habit.id,
            date = date,
            isCompleted = newCompleted,
            updatedAt = now,
            deletedAt = null,
        )

        return try {
            val userId = remote.requireUserId()
            remote.upsertRecord(recordEntity.toDto(userId))
            habitDao.upsertHabitRecord(recordEntity)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "toggleHabitCompletion failed habitId=${habit.id}", e)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    override fun getHabitById(id: String): Flow<Habit?> =
        habitDao.getHabitById(id).map { entity ->
            entity?.let { with(habitMapper) { it.toDomain() } }
        }

    override fun getRecordsForHabit(habitId: String): Flow<List<HabitRecord>> =
        habitDao.getRecordsForHabit(habitId).map { records ->
            records.map { with(habitMapper) { it.toDomain() } }
        }

    override suspend fun insertHabit(habit: Habit): DataResult<Unit, AppError> {
        if (!networkChecker.isOnline()) return DataResult.Error(AppError.Common.Network)
        val now = Instant.now()
        return try {
            val userId = remote.requireUserId()
            remote.upsertHabit(with(HabitRemoteMapper) { habit.toDto(userId, updatedAt = now) })
            with(habitMapper) {
                habitDao.insertHabit(habit.toEntity(updatedAt = now.toEpochMillis()))
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "insertHabit failed id=${habit.id}", e)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    override suspend fun updateHabit(habit: Habit): DataResult<Unit, AppError> {
        if (!networkChecker.isOnline()) return DataResult.Error(AppError.Common.Network)
        val now = Instant.now()
        return try {
            val userId = remote.requireUserId()
            remote.upsertHabit(with(HabitRemoteMapper) { habit.toDto(userId, updatedAt = now) })
            with(habitMapper) {
                habitDao.updateHabit(habit.toEntity(updatedAt = now.toEpochMillis()))
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateHabit failed id=${habit.id}", e)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    override suspend fun deleteHabit(habit: Habit): DataResult<Unit, AppError> {
        if (!networkChecker.isOnline()) return DataResult.Error(AppError.Common.Network)
        val now = Instant.now()
        return try {
            val userId = remote.requireUserId()
            remote.upsertHabit(
                with(HabitRemoteMapper) {
                    habit.toDto(userId, updatedAt = now, deletedAt = now)
                },
            )
            with(habitMapper) {
                habitDao.insertHabit(
                    habit.toEntity(
                        updatedAt = now.toEpochMillis(),
                        deletedAt = now.toEpochMillis(),
                    ),
                )
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteHabit failed id=${habit.id}", e)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    override suspend fun syncFromRemote(): DataResult<Unit, AppError> {
        if (!networkChecker.isOnline()) return DataResult.Error(AppError.Common.Network)
        return try {
            val remoteHabits = remote.fetchHabits()
            val remoteRecords = remote.fetchRecords()
            habitDao.clearHabitRecords()
            habitDao.clearHabits()
            remoteHabits.forEach { dto ->
                habitDao.insertHabit(dto.toEntity())
            }
            remoteRecords.forEach { dto ->
                habitDao.upsertHabitRecord(dto.toEntity())
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "syncFromRemote failed", e)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    companion object {
        private const val TAG = "HabitRepository"
    }
}
