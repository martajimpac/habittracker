package com.marta.habittracker.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class MonthCalendarTest {

    @Test
    fun `october 2026 starts on the previous monday and covers the month`() {
        val dates = monthCalendarDates(YearMonth.of(2026, 10))

        assertEquals(DayOfWeek.MONDAY, dates.first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 28), dates.first())
        assertEquals(LocalDate.of(2026, 11, 1), dates.last())
        assertEquals(0, dates.size % 7)
        assertEquals(31, dates.count { YearMonth.from(it) == YearMonth.of(2026, 10) })
    }

    @Test
    fun `month starting on monday has no leading days`() {
        val dates = monthCalendarDates(YearMonth.of(2026, 6))

        assertEquals(LocalDate.of(2026, 6, 1), dates.first())
        assertEquals(DayOfWeek.MONDAY, dates.first().dayOfWeek)
    }

    @Test
    fun `title capitalizes the standalone month name`() {
        assertEquals(
            "Octubre 2026",
            monthCalendarTitle(YearMonth.of(2026, 10), Locale.forLanguageTag("es")),
        )
    }
}
