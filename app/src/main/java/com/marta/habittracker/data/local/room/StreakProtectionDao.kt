package com.marta.habittracker.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marta.habittracker.data.local.room.entities.StreakBaselineEntity
import com.marta.habittracker.data.local.room.entities.StreakProtectionEventEntity

@Dao
interface StreakProtectionDao {
    @Query("SELECT * FROM streak_protection_events")
    suspend fun events(): List<StreakProtectionEventEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(events: List<StreakProtectionEventEntity>)

    @Query("SELECT * FROM streak_protection_baselines")
    suspend fun baselines(): List<StreakBaselineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBaseline(baseline: StreakBaselineEntity)
}
