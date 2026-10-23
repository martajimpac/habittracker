package com.marta.habittracker.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.marta.habittracker.data.local.room.entities.HabitEntity
import com.marta.habittracker.data.local.room.entities.HabitRecordEntity
import com.marta.habittracker.data.local.room.entities.StreakBaselineEntity
import com.marta.habittracker.data.local.room.entities.StreakProtectionEventEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitRecordEntity::class,
        StreakProtectionEventEntity::class,
        StreakBaselineEntity::class,
    ],
    version = 20261023,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    abstract fun streakProtectionDao(): StreakProtectionDao
}
