package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.protectionMilestone
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

const val MAX_STREAK_PROTECTIONS = 3

enum class ProtectionEventType { GRANT, CONSUME }

data class ProtectionEvent(
    val habitId: String,
    val occasionKey: String,
    val type: ProtectionEventType,
)

enum class StreakOutcome { ONGOING, PROTECTED, BROKEN, GRANTED }

data class HabitProtectionInput(
    val habitId: String,
    val schedule: HabitSchedule,
    val daysOfWeek: Set<DayOfWeek>,
    val daysPerWeek: Int,
    val completedDates: Set<LocalDate>,
    val createdOn: LocalDate,
    val baselineStreak: Int = 0,
    val baselineProgress: Int = 0,
    val evaluateFrom: LocalDate,
)

data class HabitProtectionStatus(
    val habitId: String,
    val streak: Int,
    val bestStreak: Int,
    val progress: Int,
    val milestone: Int,
    val outcome: StreakOutcome,
)

data class ProtectionReconcileResult(
    val balance: Int,
    val habits: List<HabitProtectionStatus>,
    val newEvents: List<ProtectionEvent>,
)

/**
 * Replays closed occasions from [HabitProtectionInput.evaluateFrom].
 * Today, and a weekly goal that is still open, are not misses.
 * Events already stored are reused, so running this twice does not grant or spend twice.
 * A full inventory drops the new protection instead of banking it.
 */
fun reconcileStreakProtections(
    habits: List<HabitProtectionInput>,
    existingEvents: List<ProtectionEvent>,
    today: LocalDate,
    protectionSince: LocalDate,
): ProtectionReconcileResult {
    val known = existingEvents.distinctBy { it.identity() }.toMutableList()
    var balance = protectionBalance(known)
    val states = habits.associate { habit ->
        habit.habitId to MutableStreak(
            streak = habit.baselineStreak.coerceAtLeast(0),
            progress = habit.baselineProgress.coerceAtLeast(0),
            best = habit.baselineStreak.coerceAtLeast(0),
        )
    }.toMutableMap()
    val fresh = mutableListOf<ProtectionEvent>()
    val lastOutcome = mutableMapOf<String, StreakOutcome>()

    val occasions = habits
        .flatMap { habit -> closedOccasions(habit, today) }
        .sortedWith(compareBy<ClosedOccasion> { it.end }.thenBy { it.habitId })

    for (occasion in occasions) {
        val state = states.getValue(occasion.habitId)
        val milestone = occasion.milestone
        if (occasion.completed) {
            state.streak++
            state.progress++
            state.best = maxOf(state.best, state.streak)
            lastOutcome[occasion.habitId] = StreakOutcome.ONGOING
            if (state.progress % milestone == 0 && occasion.end >= protectionSince) {
                val grant = ProtectionEvent(occasion.habitId, occasion.key, ProtectionEventType.GRANT)
                if (known.none { it.identity() == grant.identity() }) {
                    if (balance < MAX_STREAK_PROTECTIONS) {
                        known += grant
                        fresh += grant
                        balance++
                        lastOutcome[occasion.habitId] = StreakOutcome.GRANTED
                    }
                }
            }
        } else {
            val consume = ProtectionEvent(occasion.habitId, occasion.key, ProtectionEventType.CONSUME)
            val already = known.any { it.identity() == consume.identity() }
            if (already) {
                lastOutcome[occasion.habitId] = StreakOutcome.PROTECTED
            } else if (occasion.end >= protectionSince && balance > 0) {
                known += consume
                fresh += consume
                balance--
                lastOutcome[occasion.habitId] = StreakOutcome.PROTECTED
            } else {
                state.streak = 0
                state.progress = 0
                lastOutcome[occasion.habitId] = StreakOutcome.BROKEN
            }
        }
    }

    val statuses = habits.map { habit ->
        val state = states.getValue(habit.habitId)
        val openCredit = openWeekCredit(habit, today)
        val milestone = habit.schedule.protectionMilestone()
        val streak = state.streak + openCredit
        val raw = state.progress + openCredit
        val remainder = raw % milestone
        val progress = if (remainder == 0 && openCredit > 0 && raw > 0) milestone else remainder
        HabitProtectionStatus(
            habitId = habit.habitId,
            streak = streak,
            bestStreak = maxOf(state.best, streak),
            progress = progress,
            milestone = milestone,
            outcome = lastOutcome[habit.habitId] ?: StreakOutcome.ONGOING,
        )
    }
    return ProtectionReconcileResult(
        balance = balance.coerceIn(0, MAX_STREAK_PROTECTIONS),
        habits = statuses,
        newEvents = fresh,
    )
}

fun protectionBalance(events: List<ProtectionEvent>): Int {
    val grants = events.count { it.type == ProtectionEventType.GRANT }
    val consumes = events.count { it.type == ProtectionEventType.CONSUME }
    return (grants - consumes).coerceIn(0, MAX_STREAK_PROTECTIONS)
}

private data class MutableStreak(var streak: Int, var progress: Int, var best: Int)

private data class ClosedOccasion(
    val habitId: String,
    val key: String,
    val end: LocalDate,
    val completed: Boolean,
    val milestone: Int,
)

private fun ProtectionEvent.identity(): String = "$habitId|$occasionKey|$type"

private fun closedOccasions(habit: HabitProtectionInput, today: LocalDate): List<ClosedOccasion> {
    if (today < habit.evaluateFrom && today < habit.createdOn) return emptyList()
    val start = maxOf(habit.evaluateFrom, habit.createdOn)
    return when (habit.schedule) {
        HabitSchedule.DAILY, HabitSchedule.WEEKDAYS -> dayOccasions(habit, start, today)
        HabitSchedule.WEEKLY_COUNT -> weekOccasions(habit, start, today)
    }
}

private fun dayOccasions(
    habit: HabitProtectionInput,
    start: LocalDate,
    today: LocalDate,
): List<ClosedOccasion> {
    val last = if (today in habit.completedDates) today else today.minus(DatePeriod(days = 1))
    if (last < start) return emptyList()
    val occasions = mutableListOf<ClosedOccasion>()
    var date = start
    while (date <= last) {
        val scheduled = habit.schedule == HabitSchedule.DAILY || date.dayOfWeek in habit.daysOfWeek
        if (scheduled) {
            occasions += ClosedOccasion(
                habitId = habit.habitId,
                key = date.toString(),
                end = date,
                completed = date in habit.completedDates,
                milestone = habit.schedule.protectionMilestone(),
            )
        }
        date = date.plus(DatePeriod(days = 1))
    }
    return occasions
}

private fun weekOccasions(
    habit: HabitProtectionInput,
    start: LocalDate,
    today: LocalDate,
): List<ClosedOccasion> {
    val target = habit.daysPerWeek.coerceIn(1, 7)
    val occasions = mutableListOf<ClosedOccasion>()
    var weekStart = mondayOf(start)
    repeat(104) {
        val weekEnd = weekStart.plus(DatePeriod(days = 6))
        if (weekEnd >= today) return occasions
        if (weekEnd >= habit.createdOn && weekEnd >= start) {
            val availableStart = maxOf(weekStart, habit.createdOn)
            val available = inclusiveDays(availableStart, weekEnd)
            val needed = minOf(target, available)
            val done = habit.completedDates.count { it in availableStart..weekEnd }
            occasions += ClosedOccasion(
                habitId = habit.habitId,
                key = "week:$weekStart",
                end = weekEnd,
                completed = done >= needed,
                milestone = habit.schedule.protectionMilestone(),
            )
        }
        weekStart = weekStart.plus(DatePeriod(days = 7))
    }
    return occasions
}

private fun openWeekCredit(habit: HabitProtectionInput, today: LocalDate): Int {
    if (habit.schedule != HabitSchedule.WEEKLY_COUNT || today < habit.createdOn) return 0
    val target = habit.daysPerWeek.coerceIn(1, 7)
    val weekStart = mondayOf(today)
    val weekEnd = weekStart.plus(DatePeriod(days = 6))
    val availableStart = maxOf(weekStart, habit.createdOn)
    val needed = minOf(target, inclusiveDays(availableStart, weekEnd))
    val done = habit.completedDates.count { it in availableStart..weekEnd }
    return if (done >= needed) 1 else 0
}

private fun mondayOf(date: LocalDate): LocalDate =
    date.minus(DatePeriod(days = date.dayOfWeek.ordinal))

private fun inclusiveDays(start: LocalDate, end: LocalDate): Int {
    if (end < start) return 0
    var count = 0
    var cursor = start
    while (cursor <= end) {
        count++
        cursor = cursor.plus(DatePeriod(days = 1))
    }
    return count
}
