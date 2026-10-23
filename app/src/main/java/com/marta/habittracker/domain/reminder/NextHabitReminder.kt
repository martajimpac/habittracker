package com.marta.habittracker.domain.reminder

import com.marta.habittracker.core.toKotlinSet
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.resolvedSchedule
import com.marta.habittracker.domain.usecase.completedInWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate

object NextHabitReminder {
    private val TimePattern = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")

    fun nextTrigger(
        now: ZonedDateTime,
        reminderTime: String?,
        daysOfWeek: Set<KotlinDayOfWeek>,
    ): Instant? {
        val time = parse(reminderTime) ?: return null
        if (daysOfWeek.isEmpty()) return null
        for (plusDays in 0..7) {
            val candidate = now.toLocalDate().plusDays(plusDays.toLong()).atTime(time).atZone(now.zone)
            if (candidate.isBefore(now) || candidate.isEqual(now)) continue
            val kotlinDow = listOf(candidate.dayOfWeek).toKotlinSet().single()
            if (kotlinDow in daysOfWeek) return candidate.toInstant()
        }
        return null
    }

    fun shouldCompleteFromReminder(habit: Habit, today: KotlinLocalDate): Boolean =
        habit.records.none { it.date == today && it.isCompleted }

    fun shouldNotify(habit: Habit, today: KotlinLocalDate): Boolean {
        if (habit.reminderTime.isNullOrBlank()) return false
        if (!shouldCompleteFromReminder(habit, today)) return false
        return when (habit.resolvedSchedule()) {
            HabitSchedule.WEEKDAYS -> today.dayOfWeek in habit.daysOfWeek
            HabitSchedule.WEEKLY_COUNT ->
                habit.completedInWeek(today).size < habit.daysPerWeek.coerceIn(1, 7)
            HabitSchedule.DAILY -> true
        }
    }

    private fun parse(value: String?): LocalTime? {
        val match = TimePattern.matchEntire(value?.trim().orEmpty()) ?: return null
        return LocalTime.of(match.groupValues[1].toInt(), match.groupValues[2].toInt())
    }
}
