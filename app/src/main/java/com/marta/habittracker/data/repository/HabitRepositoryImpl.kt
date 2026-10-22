package com.marta.habittracker.data.repository

import android.util.Log
import com.marta.habittracker.core.IdGenerator
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
import com.marta.habittracker.core.toKotlin
import com.marta.habittracker.domain.model.sortedForSchedule
import com.marta.habittracker.domain.usecase.shownOn
import com.marta.habittracker.domain.reminder.HabitReminderScheduler
import com.marta.habittracker.domain.reminder.afterHabitDeleted
import com.marta.habittracker.domain.reminder.afterHabitPersisted
import com.marta.habittracker.domain.reminder.afterHabitsSynced
import com.marta.habittracker.domain.repository.HabitRepository as DomainHabitRepository
import com.marta.habittracker.domain.widget.AppWidgetRefresher
import com.marta.habittracker.domain.widget.onHabitSyncFinished
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val habitMapper: HabitMapper,
    private val remote: HabitRemoteDataSource,
    private val networkChecker: NetworkChecker,
    private val reminderScheduler: HabitReminderScheduler,
    private val widgetRefresher: AppWidgetRefresher,
) : DomainHabitRepository {

    override fun getHabitsWithStatus(date: LocalDate): Flow<List<Habit>> {
        return habitDao.getHabitsWithRecords()
            .map { habitsList ->
                habitsList
                    .map { habitWithRecords ->
                        val habit = habitMapper.map(habitWithRecords)
                        val isCompleted = habitWithRecords.records.any {
                            it.deletedAt == null && it.date == date && it.isCompleted
                        }
                        habit.copy(isCompleted = isCompleted)
                    }
                    .filter { it.shownOn(date.toKotlin()) }
                    .sortedForSchedule()
            }
    }

    override fun getAllHabitsWithRecords(): Flow<List<Habit>> =
        habitDao.getHabitsWithRecords().map { habitsList ->
            habitsList.map(habitMapper::map).sortedForSchedule()
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
            id = IdGenerator.newId(),
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
            runReminderHook("afterHabitPersisted insert id=${habit.id}") {
                afterHabitPersisted(habit, reminderScheduler)
            }
            DataResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
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
            runReminderHook("afterHabitPersisted update id=${habit.id}") {
                afterHabitPersisted(habit, reminderScheduler)
            }
            DataResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
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
            runReminderHook("afterHabitDeleted id=${habit.id}") {
                afterHabitDeleted(habit.id, reminderScheduler)
            }
            DataResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
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
            val inserted = remoteHabits.map { dto -> dto.toEntity() }
            inserted.forEach { entity ->
                habitDao.insertHabit(entity)
            }
            remoteRecords.forEach { dto ->
                habitDao.upsertHabitRecord(dto.toEntity())
            }
            val syncedHabits = inserted
                .filter { it.deletedAt == null }
                .map { entity ->
                    with(habitMapper) { entity.toDomain() }
                }
            runReminderHook("afterHabitsSynced") {
                afterHabitsSynced(syncedHabits, reminderScheduler)
            }
            val success = DataResult.Success(Unit)
            onHabitSyncFinished(success) { widgetRefresher.refreshAll() }
            success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "syncFromRemote failed", e)
            DataResult.Error(AppError.Common.Unknown)
        }
    }

    override suspend fun clearLocal() {
        try {
            Log.d(TAG, "Clearing local habits")
            val habitIds = habitDao.getAllHabits().first().map { it.id }
            habitDao.clearHabitRecords()
            habitDao.clearHabits()
            habitIds.forEach { habitId ->
                runReminderHook("cancel reminder after sign-out id=$habitId") {
                    reminderScheduler.cancel(habitId)
                }
            }
            widgetRefresher.refreshAll()
            Log.d(TAG, "Cleared local habits count=${habitIds.size}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "clearLocal failed", e)
            throw e
        }
    }

    private fun runReminderHook(operation: String, block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$operation failed", e)
        }
    }

    companion object {
        private const val TAG = "HabitRepository"
    }
}
