package com.marta.habittracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.marta.habittracker.data.local.room.HabitDao
import com.marta.habittracker.data.local.room.HabitDatabase
import com.marta.habittracker.data.local.room.StreakProtectionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_20261022_20261023 = object : Migration(20261022, 20261023) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE habits ADD COLUMN scheduleMode TEXT NOT NULL DEFAULT ''")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS streak_protection_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    habitId TEXT NOT NULL,
                    occasionKey TEXT NOT NULL,
                    type TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_streak_protection_events_habitId_occasionKey_type
                ON streak_protection_events (habitId, occasionKey, type)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS streak_protection_baselines (
                    habitId TEXT NOT NULL PRIMARY KEY,
                    baselineStreak INTEGER NOT NULL,
                    baselineProgress INTEGER NOT NULL,
                    since TEXT NOT NULL,
                    scheduleKey TEXT NOT NULL,
                    currentStreak INTEGER NOT NULL,
                    currentProgress INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_20261019_20261022 = object : Migration(20261019, 20261022) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE habits ADD COLUMN daysPerWeek INTEGER NOT NULL DEFAULT 7",
            )
        }
    }

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
            .addMigrations(
                MIGRATION_20260731_20261019,
                MIGRATION_20261019_20261022,
                MIGRATION_20261022_20261023,
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideHabitDao(database: HabitDatabase): HabitDao {
        return database.habitDao()
    }

    @Provides
    fun provideStreakProtectionDao(database: HabitDatabase): StreakProtectionDao {
        return database.streakProtectionDao()
    }
}
