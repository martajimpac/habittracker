package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ComputeChallengeScoreTest {

    private val habitId = "habit-1"

    // Jul 6 2026 is a Monday; 14 calendar days: Jul 6–19.
    private val startsAt = Instant.parse("2026-07-06T00:00:00Z")
    private val endsAt = Instant.parse("2026-07-20T00:00:00Z")

    private fun done(vararg days: Int) = days.mapIndexed { index, day ->
        HabitRecord("r$index", habitId, LocalDate(2026, 7, day), true)
    }

    @Test
    fun `completion score is completed days over all challenge days`() {
        val score = computeChallengeScore(
            criteria = ChallengeCriteria.CompletionPct,
            startsAt = startsAt,
            endsAt = endsAt,
            records = done(6, 7, 8, 10),
            today = LocalDate(2026, 7, 10),
        )

        assertEquals(ChallengeScore(done = 4, total = 14), score)
    }

    @Test
    fun `all days score uses the same full-window total`() {
        val score = computeChallengeScore(
            criteria = ChallengeCriteria.AllDays,
            startsAt = startsAt,
            endsAt = endsAt,
            records = done(6, 7),
            today = LocalDate(2026, 7, 8),
        )

        assertEquals(ChallengeScore(done = 2, total = 14), score)
    }

    @Test
    fun `weekly quota caps both done and total per week`() {
        val score = computeChallengeScore(
            criteria = ChallengeCriteria.CompletionPct,
            startsAt = startsAt,
            endsAt = endsAt,
            records = done(6, 7, 8, 9),
            today = LocalDate(2026, 7, 12),
            daysPerWeek = 3,
        )

        assertEquals(ChallengeScore(done = 3, total = 6), score)
    }

    @Test
    fun `streak score is the current streak over challenge length`() {
        val score = computeChallengeScore(
            criteria = ChallengeCriteria.Streak,
            startsAt = startsAt,
            endsAt = endsAt,
            records = done(6, 8, 9, 10),
            today = LocalDate(2026, 7, 10),
        )

        assertEquals(ChallengeScore(done = 3, total = 14), score)
    }

    @Test
    fun `nothing is done before the challenge starts`() {
        val score = computeChallengeScore(
            criteria = ChallengeCriteria.CompletionPct,
            startsAt = startsAt,
            endsAt = endsAt,
            records = done(6),
            today = LocalDate(2026, 7, 5),
        )

        assertEquals(ChallengeScore(done = 0, total = 14), score)
    }

    @Test
    fun `score ordering matches progress ordering`() {
        val today = LocalDate(2026, 7, 12)
        val mine = done(6, 7, 8, 9, 10)
        val theirs = done(6, 7, 8)
        ChallengeCriteria.entries.forEach { criteria ->
            val myScore = computeChallengeScore(criteria, startsAt, endsAt, mine, today).done
            val theirScore = computeChallengeScore(criteria, startsAt, endsAt, theirs, today).done
            val myPct = computeChallengeProgress(criteria, startsAt, endsAt, mine, today)
            val theirPct = computeChallengeProgress(criteria, startsAt, endsAt, theirs, today)
            assertEquals(criteria.name, myPct.compareTo(theirPct).coerceIn(-1, 1), myScore.compareTo(theirScore).coerceIn(-1, 1))
        }
    }
}
