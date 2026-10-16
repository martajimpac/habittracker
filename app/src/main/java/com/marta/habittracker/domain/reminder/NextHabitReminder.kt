package com.marta.habittracker.domain.reminder

import com.marta.habittracker.core.toKotlinSet
import com.marta.habittracker.domain.model.Habit
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

    fun shouldNotify(habit: Habit, today: KotlinLocalDate): Boolean {
        if (habit.reminderTime.isNullOrBlank()) return false
        if (today.dayOfWeek !in habit.daysOfWeek) return false
        return habit.records.none { it.date == today && it.isCompleted }
    }

    private fun parse(value: String?): LocalTime? {
        val match = TimePattern.matchEntire(value?.trim().orEmpty()) ?: return null
        return LocalTime.of(match.groupValues[1].toInt(), match.groupValues[2].toInt())
    }
}
