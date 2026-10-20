package com.marta.habittracker.presentation.widgets.today

import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TodayWidgetSnapshotTest {

    private val today = LocalDate.of(2026, 8, 5) // Wednesday

    @Test
    fun `lists only habits scheduled today and opens the first one that is not done`() {
        val habits = listOf(
            sampleHabit(
                id = "done",
                name = "Done",
                days = setOf(KotlinDayOfWeek.WEDNESDAY),
                records = listOf(HabitRecord("1", "done", KotlinLocalDate(2026, 8, 5), true)),
            ),
            sampleHabit(
                id = "later",
                name = "Later",
                days = setOf(KotlinDayOfWeek.THURSDAY),
            ),
            sampleHabit(
                id = "pending",
                name = "Pending",
                days = setOf(KotlinDayOfWeek.WEDNESDAY),
                records = listOf(HabitRecord("2", "pending", KotlinLocalDate(2026, 8, 5), false)),
            ),
        )

        val snapshot = todayWidgetSnapshot(habits, today)

        assertEquals(listOf("done", "pending"), snapshot.items.map { it.id })
        assertEquals(true, snapshot.items.first().completed)
        assertEquals("pending", snapshot.firstPendingId)
    }

    @Test
    fun `all done or none scheduled leaves no pending habit`() {
        val done = sampleHabit(
            id = "done",
            name = "Done",
            days = setOf(KotlinDayOfWeek.WEDNESDAY),
            records = listOf(HabitRecord("1", "done", KotlinLocalDate(2026, 8, 5), true)),
        )

        assertNull(todayWidgetSnapshot(listOf(done), today).firstPendingId)
        assertEquals(emptyList<String>(), todayWidgetSnapshot(emptyList(), today).items.map { it.id })
    }

    private fun sampleHabit(
        id: String,
        name: String,
        days: Set<KotlinDayOfWeek>,
        records: List<HabitRecord> = emptyList(),
    ) = Habit(
        id = id,
        name = name,
        description = null,
        daysOfWeek = days,
        icon = "water_drop",
        colorHex = "#6750A4",
        reminderTime = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        records = records,
    )
}
