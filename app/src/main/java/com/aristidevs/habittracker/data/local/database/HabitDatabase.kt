package com.aristidevs.habittracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aristidevs.habittracker.data.local.database.entities.HabitEntity
import com.aristidevs.habittracker.data.local.database.entities.HabitRecordEntity
import com.aristidevs.habittracker.data.local.database.entities.HabitWithRecordsEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitRecordEntity::class
    ],
    version = 1)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}


