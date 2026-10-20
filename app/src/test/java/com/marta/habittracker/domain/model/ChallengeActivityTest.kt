package com.marta.habittracker.domain.model

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ChallengeActivityTest {

    @Test
    fun `weeks run from the start monday through the end sunday`() {
        val weeks = challengeWeekRows(
            startsAt = Instant.parse("2026-01-07T00:00:00Z"),
            endsAt = Instant.parse("2026-01-14T00:00:00Z"),
        )

        assertEquals(2, weeks.size)
        assertEquals(LocalDate(2026, 1, 5), weeks[0].days.first().date)
        assertEquals(LocalDate(2026, 1, 11), weeks[0].days.last().date)
        assertFalse(weeks[0].days[0].inChallenge)
        assertTrue(weeks[0].days[2].isStart)
        assertTrue(weeks[0].days[2].inChallenge)
        assertTrue(weeks[1].days[1].isEnd)
        assertFalse(weeks[1].days[2].inChallenge)
    }

    @Test
    fun `a single day is both the start and the end`() {
        val day = challengeWeekRows(
            startsAt = Instant.parse("2026-01-07T00:00:00Z"),
            endsAt = Instant.parse("2026-01-08T00:00:00Z"),
        ).single().days[2]

        assertTrue(day.isStart)
        assertTrue(day.isEnd)
        assertTrue(day.inChallenge)
    }

    @Test
    fun `an empty window has no weeks`() {
        assertTrue(
            challengeWeekRows(
                startsAt = Instant.parse("2026-01-08T00:00:00Z"),
                endsAt = Instant.parse("2026-01-08T00:00:00Z"),
            ).isEmpty(),
        )
    }

    @Test
    fun `only completed days inside the window count`() {
        val records = listOf(
            record(LocalDate(2026, 1, 6), completed = true),
            record(LocalDate(2026, 1, 7), completed = true),
            record(LocalDate(2026, 1, 8), completed = false),
            record(LocalDate(2026, 1, 14), completed = true),
        )

        assertEquals(
            setOf(LocalDate(2026, 1, 7)),
            completedDatesInChallenge(
                records = records,
                startsAt = Instant.parse("2026-01-07T00:00:00Z"),
                endsAt = Instant.parse("2026-01-14T00:00:00Z"),
            ),
        )
    }

    private fun record(date: LocalDate, completed: Boolean) = HabitRecord(
        habitId = "habit",
        date = date,
        isCompleted = completed,
    )
}
