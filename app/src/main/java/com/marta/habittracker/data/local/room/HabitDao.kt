package com.marta.habittracker.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.marta.habittracker.data.local.room.entities.HabitEntity
import com.marta.habittracker.data.local.room.entities.HabitRecordEntity
import com.marta.habittracker.data.local.room.entities.HabitWithRecordsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE deletedAt IS NULL")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE daysOfWeek LIKE '%' || :dayOfWeek || '%' AND deletedAt IS NULL")
    fun getHabitsForDayOfWeek(dayOfWeek: Int): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitRecord(record: HabitRecordEntity)

    @Query(
        "SELECT * FROM habit_records WHERE habitId = :habitId AND date = :date AND deletedAt IS NULL LIMIT 1",
    )
    suspend fun getRecordForHabitOnDate(habitId: String, date: LocalDate): HabitRecordEntity?

    @Query("SELECT * FROM habits WHERE id = :id AND deletedAt IS NULL")
    fun getHabitById(id: String): Flow<HabitEntity?>

    @Query(
        "SELECT * FROM habit_records WHERE habitId = :habitId AND deletedAt IS NULL ORDER BY date DESC",
    )
    fun getRecordsForHabit(habitId: String): Flow<List<HabitRecordEntity>>

    @Transaction
    @Query("SELECT * FROM habits WHERE deletedAt IS NULL")
    fun getHabitsWithRecords(): Flow<List<HabitWithRecordsEntity>>

    @Transaction
    @Query("SELECT * FROM habits WHERE id = :id AND deletedAt IS NULL")
    fun getHabitWithRecords(id: String): Flow<HabitWithRecordsEntity>

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("DELETE FROM habit_records")
    suspend fun clearHabitRecords()
}
