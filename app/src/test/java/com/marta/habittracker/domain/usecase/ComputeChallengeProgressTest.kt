package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ComputeChallengeProgressTest {

    private val habitId = "habit-1"

    @Test
    fun `completion_pct is completed days over total challenge days`() {
        val startsAt = Instant.parse("2026-07-01T00:00:00Z")
        val endsAt = Instant.parse("2026-07-08T00:00:00Z") // 7 calendar days: Jul 1–7
        val records = listOf(
            HabitRecord("1", habitId, LocalDate(2026, 7, 1), true),
            HabitRecord("2", habitId, LocalDate(2026, 7, 2), true),
            HabitRecord("3", habitId, LocalDate(2026, 7, 3), false),
        )

        val pct = computeChallengeProgress(
            criteria = ChallengeCriteria.CompletionPct,
            startsAt = startsAt,
            endsAt = endsAt,
            records = records,
            today = LocalDate(2026, 7, 4),
        )

        // 2 completed in [Jul 1, Jul 4] out of 7 total challenge days
        assertEquals(28, pct)
    }

    @Test
    fun `completion_pct counts only records inside challenge window`() {
        val startsAt = Instant.parse("2026-07-01T00:00:00Z")
        val endsAt = Instant.parse("2026-07-08T00:00:00Z")
        val records = listOf(
            HabitRecord("0", habitId, LocalDate(2026, 6, 30), true),
            HabitRecord("1", habitId, LocalDate(2026, 7, 1), true),
            HabitRecord("2", habitId, LocalDate(2026, 7, 8), true),
        )

        val pct = computeChallengeProgress(
            criteria = ChallengeCriteria.CompletionPct,
            startsAt = startsAt,
            endsAt = endsAt,
            records = records,
            today = LocalDate(2026, 7, 7),
        )

        assertEquals(14, pct) // 1 of 7
    }

    @Test
    fun `completion_pct after challenge ends uses full window`() {
        val startsAt = Instant.parse("2026-07-01T00:00:00Z")
        val endsAt = Instant.parse("2026-07-08T00:00:00Z")
        val records = (1..7).map { day ->
            HabitRecord("$day", habitId, LocalDate(2026, 7, day), true)
        }

        val pct = computeChallengeProgress(
            criteria = ChallengeCriteria.CompletionPct,
            startsAt = startsAt,
            endsAt = endsAt,
            records = records,
            today = LocalDate(2026, 7, 15),
        )

        assertEquals(100, pct)
    }

    @Test
    fun `all_days is percent of required days completed so far`() {
        val startsAt = Instant.parse("2026-07-01T00:00:00Z")
        val endsAt = Instant.parse("2026-07-08T00:00:00Z")
        val records = listOf(
            HabitRecord("1", habitId, LocalDate(2026, 7, 1), true),
            HabitRecord("2", habitId, LocalDate(2026, 7, 2), true),
            HabitRecord("3", habitId, LocalDate(2026, 7, 3), false),
        )

        val pct = computeChallengeProgress(
            criteria = ChallengeCriteria.AllDays,
            startsAt = startsAt,
            endsAt = endsAt,
            records = records,
            today = LocalDate(2026, 7, 4),
        )

        // Required days Jul 1–4: 2 of 4 completed
        assertEquals(50, pct)
    }

    @Test
    fun `all_days returns 100 when every required day is completed`() {
        val startsAt = Instant.parse("2026-07-01T00:00:00Z")
        val endsAt = Instant.parse("2026-07-08T00:00:00Z")
        val records = (1..4).map { day ->
            HabitRecord("$day", habitId, LocalDate(2026, 7, day), true)
        }

        val pct = computeChallengeProgress(
            criteria = ChallengeCriteria.AllDays,
            startsAt = startsAt,
            endsAt = endsAt,
            records = records,
            today = LocalDate(2026, 7, 4),
        )

        assertEquals(100, pct)
    }

    @Test
    fun `streak maps current in-window streak to percent of total challenge days`() {
        val startsAt = Instant.parse("2026-07-01T00:00:00Z")
        val endsAt = Instant.parse("2026-07-08T00:00:00Z")
        val records = listOf(
            HabitRecord("1", habitId, LocalDate(2026, 7, 1), true),
            HabitRecord("2", habitId, LocalDate(2026, 7, 2), true),
            HabitRecord("3", habitId, LocalDate(2026, 7, 3), true),
        )

        val pct = computeChallengeProgress(
            criteria = ChallengeCriteria.Streak,
            startsAt = startsAt,
            endsAt = endsAt,
            records = records,
            today = LocalDate(2026, 7, 3),
        )

        // streak 3 / 7 total days = 42%
        assertEquals(42, pct)
    }

    @Test
    fun `streak returns zero when streak is broken`() {
        val startsAt = Instant.parse("2026-07-01T00:00:00Z")
        val endsAt = Instant.parse("2026-07-08T00:00:00Z")
        val records = listOf(
            HabitRecord("1", habitId, LocalDate(2026, 7, 1), true),
            HabitRecord("2", habitId, LocalDate(2026, 7, 2), true),
            HabitRecord("3", habitId, LocalDate(2026, 7, 3), false),
        )

        val pct = computeChallengeProgress(
            criteria = ChallengeCriteria.Streak,
            startsAt = startsAt,
            endsAt = endsAt,
            records = records,
            today = LocalDate(2026, 7, 4),
        )

        assertEquals(0, pct)
    }

    @Test
    fun `habit streak mirrors Home consecutive completed days ending today`() {
        val records = listOf(
            HabitRecord("1", habitId, LocalDate(2026, 7, 28), true),
            HabitRecord("2", habitId, LocalDate(2026, 7, 29), false),
            HabitRecord("3", habitId, LocalDate(2026, 7, 30), true),
            HabitRecord("4", habitId, LocalDate(2026, 7, 31), true),
        )

        assertEquals(2, calculateHabitStreak(records, LocalDate(2026, 7, 31)))
    }

    @Test
    fun `challengeDaysLeft counts remaining inclusive challenge days after today`() {
        val startsAt = Instant.parse("2026-07-01T00:00:00Z")
        val endsAt = Instant.parse("2026-07-08T00:00:00Z")

        assertEquals(3, challengeDaysLeft(startsAt, endsAt, LocalDate(2026, 7, 4)))
        assertEquals(0, challengeDaysLeft(startsAt, endsAt, LocalDate(2026, 7, 7)))
        assertEquals(0, challengeDaysLeft(startsAt, endsAt, LocalDate(2026, 7, 8)))
    }

    @Test
    fun `challengeDaysLeft returns zero before challenge starts`() {
        val startsAt = Instant.parse("2026-07-01T00:00:00Z")
        val endsAt = Instant.parse("2026-07-08T00:00:00Z")

        assertEquals(0, challengeDaysLeft(startsAt, endsAt, LocalDate(2026, 6, 30)))
    }

    @Test
    fun `progress returns zero before challenge starts`() {
        val startsAt = Instant.parse("2026-07-01T00:00:00Z")
        val endsAt = Instant.parse("2026-07-08T00:00:00Z")

        val pct = computeChallengeProgress(
            criteria = ChallengeCriteria.CompletionPct,
            startsAt = startsAt,
            endsAt = endsAt,
            records = emptyList(),
            today = LocalDate(2026, 6, 30),
        )

        assertEquals(0, pct)
    }
}
