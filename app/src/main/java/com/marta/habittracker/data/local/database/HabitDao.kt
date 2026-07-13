package com.marta.habittracker.data.local.database

import androidx.room.*
import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.data.local.database.entities.HabitRecordEntity
import com.marta.habittracker.data.local.database.entities.HabitWithRecordsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {

    /* --- Operaciones de Configuración (HabitEntity) --- */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long // Retorna el ID generado

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitEntity>>

    /**
     * Busca los hábitos que deben realizarse un día específico.
     * Como guardamos los días como "1,2,3", usamos LIKE para buscar el número del día.
     */
    @Query("SELECT * FROM habits WHERE daysOfWeek LIKE '%' || :dayOfWeek || '%'")
    fun getHabitsForDayOfWeek(dayOfWeek: Int): Flow<List<HabitEntity>>


    /* --- Operaciones de Progreso (HabitRecordEntity) --- */

    /**
     * Marca un hábito como completado o pendiente.
     * Usamos OnConflictStrategy.REPLACE para que si ya existe un registro
     * para ese día, simplemente lo actualice.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitRecord(record: HabitRecordEntity)

    /**
     * Borra el registro de cumplimiento si el usuario desmarca la tarea.
     */
    @Query("DELETE FROM habit_records WHERE habitId = :habitId AND date = :date")
    suspend fun deleteHabitRecord(habitId: Long, date: LocalDate)



    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: Long): Flow<HabitEntity?>

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId ORDER BY date DESC")
    fun getRecordsForHabit(habitId: Long): Flow<List<HabitRecordEntity>>

    @Transaction
    @Query("SELECT * FROM habits")
    fun getHabitsWithRecords(): Flow<List<HabitWithRecordsEntity>>

    @Transaction
    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitWithRecords(id: Long): Flow<HabitWithRecordsEntity>
}

