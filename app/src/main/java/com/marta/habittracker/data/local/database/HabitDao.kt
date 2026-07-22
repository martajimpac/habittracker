package com.marta.habittracker.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.data.local.database.entities.HabitRecordEntity
import com.marta.habittracker.data.local.database.entities.HabitWithRecordsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE daysOfWeek LIKE '%' || :dayOfWeek || '%'")
    fun getHabitsForDayOfWeek(dayOfWeek: Int): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitRecord(record: HabitRecordEntity)

    @Query("DELETE FROM habit_records WHERE habitId = :habitId AND date = :date")
    suspend fun deleteHabitRecord(habitId: String, date: LocalDate)

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: String): Flow<HabitEntity?>

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId ORDER BY date DESC")
    fun getRecordsForHabit(habitId: String): Flow<List<HabitRecordEntity>>

    @Transaction
    @Query("SELECT * FROM habits")
    fun getHabitsWithRecords(): Flow<List<HabitWithRecordsEntity>>

    @Transaction
    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitWithRecords(id: String): Flow<HabitWithRecordsEntity>
}
