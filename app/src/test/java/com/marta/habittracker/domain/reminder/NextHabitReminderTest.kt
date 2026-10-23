package com.marta.habittracker.domain.reminder

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class NextHabitReminderTest {
    private val zone = ZoneId.of("Europe/Madrid")

    @Test
    fun `today later hour on scheduled day is used`() {
        val now = ZonedDateTime.of(2026, 10, 9, 7, 0, 0, 0, zone) // Friday
        val next = NextHabitReminder.nextTrigger(
            now = now,
            reminderTime = "08:00",
            daysOfWeek = setOf(KotlinDayOfWeek.FRIDAY),
        )
        assertEquals(ZonedDateTime.of(2026, 10, 9, 8, 0, 0, 0, zone).toInstant(), next)
    }

    @Test
    fun `hour already passed today jumps to next scheduled weekday`() {
        val now = ZonedDateTime.of(2026, 10, 9, 9, 0, 0, 0, zone) // Friday
        val next = NextHabitReminder.nextTrigger(
            now = now,
            reminderTime = "08:00",
            daysOfWeek = setOf(KotlinDayOfWeek.MONDAY, KotlinDayOfWeek.FRIDAY),
        )
        assertEquals(ZonedDateTime.of(2026, 10, 12, 8, 0, 0, 0, zone).toInstant(), next)
    }

    @Test
    fun `hour already passed wraps to same weekday next week`() {
        val now = ZonedDateTime.of(2026, 10, 9, 9, 0, 0, 0, zone) // Friday
        val next = NextHabitReminder.nextTrigger(
            now = now,
            reminderTime = "08:00",
            daysOfWeek = setOf(KotlinDayOfWeek.FRIDAY),
        )
        assertEquals(ZonedDateTime.of(2026, 10, 16, 8, 0, 0, 0, zone).toInstant(), next)
    }

    @Test
    fun `blank time or empty days returns null`() {
        val now = ZonedDateTime.of(2026, 10, 9, 7, 0, 0, 0, zone)
        assertNull(NextHabitReminder.nextTrigger(now, null, setOf(KotlinDayOfWeek.FRIDAY)))
        assertNull(NextHabitReminder.nextTrigger(now, "", setOf(KotlinDayOfWeek.FRIDAY)))
        assertNull(NextHabitReminder.nextTrigger(now, "08:00", emptySet()))
        assertNull(NextHabitReminder.nextTrigger(now, "bad", setOf(KotlinDayOfWeek.FRIDAY)))
    }

    @Test
    fun `completed record today skips notify`() {
        val today = KotlinLocalDate(2026, 10, 9)
        val habit = sampleHabit(
            days = setOf(KotlinDayOfWeek.FRIDAY),
            records = listOf(HabitRecord("r", "h", today, true)),
        )
        assertFalse(NextHabitReminder.shouldNotify(habit, today))
        assertTrue(NextHabitReminder.shouldNotify(habit.copy(records = emptyList()), today))
    }

    @Test
    fun `reminder action completes only when today is still open`() {
        val today = KotlinLocalDate(2026, 10, 9)
        val open = sampleHabit()
        val done = open.copy(records = listOf(HabitRecord("r", "h", today, true)))
        assertTrue(NextHabitReminder.shouldCompleteFromReminder(open, today))
        assertFalse(NextHabitReminder.shouldCompleteFromReminder(done, today))
    }

    companion object {
        private fun sampleHabit(
            days: Set<KotlinDayOfWeek> = setOf(KotlinDayOfWeek.FRIDAY),
            records: List<HabitRecord> = emptyList(),
        ) = Habit(
            id = "h",
            name = "Read",
            description = null,
            daysOfWeek = days,
            icon = "book",
            colorHex = "#6750A4",
            reminderTime = "08:00",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            records = records,
        )
    }
}
