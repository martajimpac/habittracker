package com.marta.habittracker.data.security

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.marta.habittracker.data.local.room.HabitDatabase
import com.marta.habittracker.data.local.room.entities.HabitEntity
import com.marta.habittracker.data.local.room.entities.HabitRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun softDeletedHabitIsHiddenFromActiveQueries() = runBlocking {
        val dao = database.habitDao()
        val habitId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        val habit = HabitEntity(
            id = habitId,
            name = "Meditation",
            description = "Private habit",
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            icon = "self_improvement",
            colorHex = "#6750A4",
            reminderTime = "07:00",
        )
        dao.insertHabit(habit)
        dao.upsertHabitRecord(
            HabitRecordEntity(
                id = "rec-1",
                habitId = habitId,
                date = LocalDate.of(2026, 6, 10),
                isCompleted = true,
            ),
        )

        dao.insertHabit(habit.copy(deletedAt = System.currentTimeMillis()))

        assertNull(dao.getHabitById(habitId).first())
        assertTrue(dao.getHabitsWithRecords().first().isEmpty())
    }

    @Test
    fun upsertHabitRecordDoesNotCreateDuplicateCompletionForSameDay() = runBlocking {
        val dao = database.habitDao()
        val habitId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        val recordId = "cccccccc-cccc-cccc-cccc-cccccccccccc"
        dao.insertHabit(
            HabitEntity(
                id = habitId,
                name = "Workout",
                description = null,
                daysOfWeek = listOf(DayOfWeek.WEDNESDAY),
                icon = "directions_run",
                colorHex = "#059669",
                reminderTime = null,
            ),
        )
        val date = LocalDate.of(2026, 6, 10)

        dao.upsertHabitRecord(
            HabitRecordEntity(
                id = recordId,
                habitId = habitId,
                date = date,
                isCompleted = true,
            ),
        )
        dao.upsertHabitRecord(
            HabitRecordEntity(
                id = recordId,
                habitId = habitId,
                date = date,
                isCompleted = false,
            ),
        )

        val records = dao.getRecordsForHabit(habitId).first()

        assertEquals("Only one record per habit and date is allowed", 1, records.size)
        assertEquals(false, records.single().isCompleted)
    }
}
