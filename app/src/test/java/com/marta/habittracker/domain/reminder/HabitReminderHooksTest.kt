package com.marta.habittracker.domain.reminder

import com.marta.habittracker.domain.model.Habit
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class HabitReminderHooksTest {
    private lateinit var scheduler: FakeHabitReminderScheduler

    @Before
    fun setUp() {
        scheduler = FakeHabitReminderScheduler()
    }

    @Test
    fun `persist with reminder time schedules and does not cancel`() {
        val habit = sampleHabit(reminderTime = "08:00")

        afterHabitPersisted(habit, scheduler)

        assertEquals(listOf(habit), scheduler.scheduled)
        assertTrue(scheduler.cancelled.isEmpty())
        assertTrue(scheduler.rescheduled.isEmpty())
    }

    @Test
    fun `persist with null reminder cancels and does not schedule`() {
        val habit = sampleHabit(reminderTime = null)

        afterHabitPersisted(habit, scheduler)

        assertEquals(listOf(habit.id), scheduler.cancelled)
        assertTrue(scheduler.scheduled.isEmpty())
        assertTrue(scheduler.rescheduled.isEmpty())
    }

    @Test
    fun `persist with blank reminder cancels and does not schedule`() {
        val habit = sampleHabit(reminderTime = "   ")

        afterHabitPersisted(habit, scheduler)

        assertEquals(listOf(habit.id), scheduler.cancelled)
        assertTrue(scheduler.scheduled.isEmpty())
        assertTrue(scheduler.rescheduled.isEmpty())
    }

    @Test
    fun `delete cancels reminder`() {
        afterHabitDeleted("habit-to-delete", scheduler)

        assertEquals(listOf("habit-to-delete"), scheduler.cancelled)
        assertTrue(scheduler.scheduled.isEmpty())
        assertTrue(scheduler.rescheduled.isEmpty())
    }

    @Test
    fun `mixed list reschedules all habits`() {
        val withTime = sampleHabit(id = "with-time", reminderTime = "08:00")
        val withoutTime = sampleHabit(id = "without-time", reminderTime = null)
        val habits = listOf(withTime, withoutTime)

        afterHabitsSynced(habits, scheduler)

        assertEquals(listOf(habits), scheduler.rescheduled)
        assertTrue(scheduler.scheduled.isEmpty())
        assertTrue(scheduler.cancelled.isEmpty())
    }

    private fun sampleHabit(
        id: String = "habit-1",
        reminderTime: String? = "08:00",
    ) = Habit(
        id = id,
        name = "Read",
        description = null,
        daysOfWeek = setOf(KotlinDayOfWeek.FRIDAY),
        icon = "book",
        colorHex = "#6750A4",
        reminderTime = reminderTime,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        records = emptyList(),
    )
}
