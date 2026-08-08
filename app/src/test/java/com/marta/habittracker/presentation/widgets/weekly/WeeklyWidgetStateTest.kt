package com.marta.habittracker.presentation.widgets.weekly

import com.marta.habittracker.domain.usecase.DaySummary
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyWidgetStateTest {

    @Test
    fun `weekly cells expose Monday through Sunday labels and percentages`() {
        val monday = LocalDate.of(2026, 8, 3)
        val summaries = (0 until 7).map { offset ->
            DaySummary(
                date = monday.plusDays(offset.toLong()),
                completed = offset,
                scheduled = 2,
            )
        }

        val cells = toWeeklyWidgetDayCells(summaries)

        assertEquals(DayOfWeek.entries, cells.map { it.dayOfWeek })
        assertEquals(listOf(0, 50, 100, 150, 200, 250, 300), cells.map { it.percent })
    }

    @Test
    fun `empty summaries produce empty cells`() {
        assertTrue(toWeeklyWidgetDayCells(emptyList()).isEmpty())
    }

    @Test
    fun `zero scheduled day maps to zero percent`() {
        val cells = toWeeklyWidgetDayCells(
            listOf(
                DaySummary(
                    date = LocalDate.of(2026, 8, 3),
                    completed = 0,
                    scheduled = 0,
                ),
            ),
        )

        assertEquals(0, cells.single().percent)
        assertEquals(DayOfWeek.MONDAY, cells.single().dayOfWeek)
    }
}
