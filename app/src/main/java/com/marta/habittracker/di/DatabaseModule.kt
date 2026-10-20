package com.marta.habittracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.marta.habittracker.data.local.room.HabitDao
import com.marta.habittracker.data.local.room.HabitDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_20260731_20261019 = object : Migration(20260731, 20261019) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE habits ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "UPDATE habits SET reminderEnabled = 1 WHERE reminderTime IS NOT NULL AND reminderTime != ''",
            )
        }
    }

    @Provides
    @Singleton
    fun provideHabitDatabase(
        @ApplicationContext context: Context,
    ): HabitDatabase {
        return Room.databaseBuilder(
            context,
            HabitDatabase::class.java,
            "habit_db",
        )
            .addMigrations(MIGRATION_20260731_20261019)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideHabitDao(database: HabitDatabase): HabitDao {
        return database.habitDao()
    }
}
