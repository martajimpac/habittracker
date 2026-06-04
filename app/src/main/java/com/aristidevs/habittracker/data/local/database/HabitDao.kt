package com.aristidevs.habittracker.data.local.database

import androidx.room.*
import com.aristidevs.habittracker.data.local.database.entities.HabitEntity
import com.aristidevs.habittracker.data.local.database.entities.HabitRecordEntity
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

    /**
     * Obtiene los hábitos de un día y
     * nos dice si están completados buscando en la tabla de registros.
     */
    @Transaction
    @Query("""
        SELECT h.*, 
               (SELECT COUNT(*) FROM habit_records r WHERE r.habitId = h.id AND r.date = :date) > 0 as isCompleted
        FROM habits h
        WHERE h.daysOfWeek LIKE '%' || :dayOfWeek || '%'
    """)
    fun getHabitsWithStatusForDay(date: LocalDate, dayOfWeek: Int): Flow<List<HabitWithStatus>>


    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: Long): Flow<HabitEntity?>

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId ORDER BY date DESC")
    fun getRecordsForHabit(habitId: Long): Flow<List<HabitRecordEntity>>
}

/**
 * POJO para recibir el resultado de la consulta combinada
 */
data class HabitWithStatus(
    @Embedded val habit: HabitEntity,
    val isCompleted: Boolean
)