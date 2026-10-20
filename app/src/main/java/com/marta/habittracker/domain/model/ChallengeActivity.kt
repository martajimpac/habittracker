package com.marta.habittracker.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.time.Instant
import java.time.ZoneOffset

data class ChallengeDayCell(
    val date: LocalDate,
    val inChallenge: Boolean,
    val isStart: Boolean,
    val isEnd: Boolean,
)

data class ChallengeWeekRow(
    val days: List<ChallengeDayCell>,
)

/** Monday-aligned weeks covering the challenge, including days outside the window. */
fun challengeWeekRows(startsAt: Instant, endsAt: Instant): List<ChallengeWeekRow> {
    val start = startsAt.toChallengeDate()
    val last = endsAt.toChallengeDate().minus(DatePeriod(days = 1))
    if (last < start) return emptyList()

    val firstMonday = start.minus(DatePeriod(days = start.dayOfWeek.daysFromMonday()))
    val lastSunday = last.plus(DatePeriod(days = 6 - last.dayOfWeek.daysFromMonday()))
    val weeks = mutableListOf<ChallengeWeekRow>()
    var monday = firstMonday
    while (monday <= lastSunday) {
        weeks += ChallengeWeekRow(
            days = (0..6).map { offset ->
                val date = monday.plus(DatePeriod(days = offset))
                ChallengeDayCell(
                    date = date,
                    inChallenge = date in start..last,
                    isStart = date == start,
                    isEnd = date == last,
                )
            },
        )
        monday = monday.plus(DatePeriod(days = 7))
    }
    return weeks
}

fun completedDatesInChallenge(
    records: List<HabitRecord>,
    startsAt: Instant,
    endsAt: Instant,
): Set<LocalDate> {
    val start = startsAt.toChallengeDate()
    val last = endsAt.toChallengeDate().minus(DatePeriod(days = 1))
    if (last < start) return emptySet()
    return records
        .filter { it.isCompleted && it.date in start..last }
        .map { it.date }
        .toSet()
}

private fun DayOfWeek.daysFromMonday(): Int = when (this) {
    DayOfWeek.MONDAY -> 0
    DayOfWeek.TUESDAY -> 1
    DayOfWeek.WEDNESDAY -> 2
    DayOfWeek.THURSDAY -> 3
    DayOfWeek.FRIDAY -> 4
    DayOfWeek.SATURDAY -> 5
    DayOfWeek.SUNDAY -> 6
}

private fun Instant.toChallengeDate(): LocalDate =
    LocalDate.parse(atZone(ZoneOffset.UTC).toLocalDate().toString())
