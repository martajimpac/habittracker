package com.aristidevs.habittracker.data.security

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.aristidevs.habittracker.data.local.database.HabitDatabase
import com.aristidevs.habittracker.data.local.database.entities.HabitEntity
import com.aristidevs.habittracker.data.local.database.entities.HabitRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitDaoSecurityInstrumentedTest {

    private lateinit var database: HabitDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun habitRecordsAreDeletedWhenHabitIsDeleted() = runBlocking {
        val dao = database.habitDao()
        val habitId = dao.insertHabit(
            HabitEntity(
                name = "Meditation",
                description = "Private habit",
                daysOfWeek = listOf(DayOfWeek.MONDAY)
            )
        )

        dao.upsertHabitRecord(
            HabitRecordEntity(
                habitId = habitId,
                date = LocalDate.of(2026, 6, 10),
                isCompleted = true
            )
        )

        val habit = dao.getHabitById(habitId).first()!!
        dao.deleteHabit(habit)

        assertTrue(
            "Habit records must not remain orphaned after deleting the parent habit",
            dao.getRecordsForHabit(habitId).first().isEmpty()
        )
    }

    @Test
    fun upsertHabitRecordDoesNotCreateDuplicateCompletionForSameDay() = runBlocking {
        val dao = database.habitDao()
        val habitId = dao.insertHabit(
            HabitEntity(
                name = "Workout",
                description = null,
                daysOfWeek = listOf(DayOfWeek.WEDNESDAY)
            )
        )
        val date = LocalDate.of(2026, 6, 10)

        dao.upsertHabitRecord(HabitRecordEntity(habitId = habitId, date = date, isCompleted = true))
        dao.upsertHabitRecord(HabitRecordEntity(habitId = habitId, date = date, isCompleted = false))

        val records = dao.getRecordsForHabit(habitId).first()

        assertEquals("Only one record per habit and date is allowed", 1, records.size)
        assertEquals(false, records.single().isCompleted)
    }
}
