package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.HabitRecord
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Computes challenge progress (0–100) from [habit_records] within the challenge window.
 *
 * Date boundaries (UTC):
 * - [startsAt] maps to inclusive [startDate].
 * - [endsAt] is exclusive; the last challenge day is the UTC calendar day before [endsAt].
 * - [today] is clamped to the challenge window when evaluating in-progress challenges.
 *
 * ## CompletionPct
 * `completedDaysInEvaluationRange / totalChallengeDays * 100`.
 * Each Monday–Sunday slice counts at most [daysPerWeek] days. `7` keeps every calendar day.
 * - Evaluation range: `[startDate, min(today, lastChallengeDay)]` (both inclusive).
 * - Only records with `isCompleted = true` inside the challenge window count.
 * - Denominator is the full challenge length in calendar days, not just elapsed days.
 *
 * ## AllDays
 * `requiredDaysCompleted / requiredDaysSoFar * 100`
 * - Required days so far: every calendar day from [startDate] through `min(today, lastChallengeDay)`.
 * - Returns 100 when every required day so far has a completed record; otherwise the proportional
 *   percentage of required days completed (0 when none).
 *
 * ## Streak
 * `currentInWindowStreak / totalChallengeDays * 100`
 * - Current streak is consecutive completed calendar days ending at
 *   `min(today, lastChallengeDay)`, walking backward, but never before [startDate].
 *   It does not follow the habit weekday schedule; that schedule is `calculateScheduledStreak`.
 * - Mapped to a percentage of total challenge days (capped at 100).
 *
 * Returns 0 when [today] is before [startDate] or when [totalChallengeDays] is zero.
 */
fun computeChallengeProgress(
    criteria: ChallengeCriteria,
    startsAt: Instant,
    endsAt: Instant,
    records: List<HabitRecord>,
    today: LocalDate,
    daysPerWeek: Int = 7,
): Int {
    val startDate = startsAt.toUtcLocalDate()
    val lastChallengeDay = endsAt.toUtcLocalDate().minus(DatePeriod(days = 1))
    val totalChallengeDays = inclusiveDaysBetween(startDate, lastChallengeDay)

    if (totalChallengeDays <= 0 || today < startDate) {
        return 0
    }

    val evaluationEnd = minOf(today, lastChallengeDay)
    val completedDates = records
        .filter { it.isCompleted && it.date in startDate..lastChallengeDay }
        .map { it.date }
        .toSet()

    val target = daysPerWeek.coerceIn(1, 7)
    return when (criteria) {
        ChallengeCriteria.CompletionPct -> {
            val (completedInRange, _) = quotaTotals(startDate, evaluationEnd, completedDates, target)
            val (_, expected) = quotaTotals(startDate, lastChallengeDay, completedDates, target)
            percent(completedInRange, expected)
        }

        ChallengeCriteria.AllDays -> {
            val (completedRequired, requiredDays) = quotaTotals(
                startDate,
                evaluationEnd,
                completedDates,
                target,
            )
            if (requiredDays == 0) 0 else percent(completedRequired, requiredDays)
        }

        ChallengeCriteria.Streak -> {
            val streak = currentStreakWithinWindow(
                completedDates = completedDates,
                today = today,
                windowStart = startDate,
                windowEnd = lastChallengeDay,
            )
            percent(streak, totalChallengeDays)
        }
    }.coerceIn(0, 100)
}

/**
 * Remaining inclusive challenge days strictly after [today], until [endsAt] (exclusive).
 *
 * Example: `endsAt = Jul 8 00:00Z` (last day Jul 7), `today = Jul 4` → 3 days left (Jul 5–7).
 * Returns 0 when [today] is before [startsAt] (challenge not started yet).
 * Returns 0 when [today] is on or after the last challenge day.
 */
fun challengeDaysLeft(startsAt: Instant, endsAt: Instant, today: LocalDate): Int {
    val startDate = startsAt.toUtcLocalDate()
    if (today < startDate) {
        return 0
    }
    val lastChallengeDay = endsAt.toUtcLocalDate().minus(DatePeriod(days = 1))
    if (today >= lastChallengeDay) {
        return 0
    }
    return inclusiveDaysBetween(today.plus(DatePeriod(days = 1)), lastChallengeDay)
}

class ComputeChallengeProgress @Inject constructor() {
    operator fun invoke(
        criteria: ChallengeCriteria,
        startsAt: Instant,
        endsAt: Instant,
        records: List<HabitRecord>,
        today: LocalDate,
        daysPerWeek: Int = 7,
    ): Int = computeChallengeProgress(criteria, startsAt, endsAt, records, today, daysPerWeek)

    fun daysLeft(startsAt: Instant, endsAt: Instant, today: LocalDate): Int =
        challengeDaysLeft(startsAt, endsAt, today)
}

/** Consecutive completed calendar days ending on [today]. Habit schedule streaks use [calculateScheduledStreak]. */
fun calculateHabitStreak(records: List<HabitRecord>, today: LocalDate): Int {
    val completedDates = records
        .filter { it.isCompleted }
        .map { it.date }
        .toSet()
    var streak = 0
    var date = today
    while (completedDates.contains(date)) {
        streak++
        date = date.minus(DatePeriod(days = 1))
    }
    return streak
}

private fun Instant.toUtcLocalDate(): LocalDate =
    LocalDate.parse(atZone(ZoneOffset.UTC).toLocalDate().toString())

private fun inclusiveDaysBetween(start: LocalDate, end: LocalDate): Int {
    if (end < start) return 0
    val javaStart = java.time.LocalDate.parse(start.toString())
    val javaEnd = java.time.LocalDate.parse(end.toString())
    return ChronoUnit.DAYS.between(javaStart, javaEnd).toInt() + 1
}

private fun countDaysInRange(
    start: LocalDate,
    end: LocalDate,
    predicate: (LocalDate) -> Boolean,
): Int {
    if (end < start) return 0
    var count = 0
    var current = start
    while (current <= end) {
        if (predicate(current)) count++
        current = current.plus(DatePeriod(days = 1))
    }
    return count
}

private fun currentStreakWithinWindow(
    completedDates: Set<LocalDate>,
    today: LocalDate,
    windowStart: LocalDate,
    windowEnd: LocalDate,
): Int {
    val effectiveToday = minOf(today, windowEnd)
    var streak = 0
    var date = effectiveToday
    while (date >= windowStart && completedDates.contains(date)) {
        streak++
        date = date.minus(DatePeriod(days = 1))
    }
    return streak
}

private fun quotaTotals(
    start: LocalDate,
    end: LocalDate,
    completedDates: Set<LocalDate>,
    daysPerWeek: Int,
): Pair<Int, Int> {
    if (end < start) return 0 to 0
    var expected = 0
    var completed = 0
    var cursor = start
    while (cursor <= end) {
        val weekEnd = cursor.plus(DatePeriod(days = 6 - cursor.dayOfWeek.ordinal))
        val sliceEnd = minOf(weekEnd, end)
        val days = inclusiveDaysBetween(cursor, sliceEnd)
        val cap = minOf(daysPerWeek, days)
        expected += cap
        val done = countDaysInRange(cursor, sliceEnd) { it in completedDates }
        completed += minOf(done, cap)
        cursor = sliceEnd.plus(DatePeriod(days = 1))
    }
    return completed to expected
}

private fun percent(numerator: Int, denominator: Int): Int {
    if (denominator <= 0) return 0
    return ((numerator.toFloat() / denominator) * 100).toInt()
}
