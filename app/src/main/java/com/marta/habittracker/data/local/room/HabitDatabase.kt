package com.marta.habittracker.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.marta.habittracker.data.local.room.entities.HabitEntity
import com.marta.habittracker.data.local.room.entities.HabitRecordEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitRecordEntity::class,
    ],
    version = 20260731,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}
