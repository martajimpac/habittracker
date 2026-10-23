package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.HabitSchedule
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class StreakProtectionTest {

    private val start = LocalDate(2026, 8, 3)

    @Test
    fun `daily habit grants one protection after seven completed days`() {
        val dates = (0 until 7).map { start.plus(DatePeriod(days = it)) }.toSet()
        val result = reconcile(daily("read", dates), today = start.plus(DatePeriod(days = 7)))

        assertEquals(1, result.balance)
        assertEquals(1, result.newEvents.count { it.type == ProtectionEventType.GRANT })
        assertEquals(7, result.habits.single().streak)
    }

    @Test
    fun `scheduled habit counts only its weekdays`() {
        val completed = setOf(LocalDate(2026, 8, 3), LocalDate(2026, 8, 4), LocalDate(2026, 8, 5))
        val result = reconcile(
            weekdays("gym", completed, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            today = LocalDate(2026, 8, 6),
        )

        assertEquals(2, result.habits.single().streak)
        assertEquals(0, result.balance)
    }

    @Test
    fun `days that are not scheduled do not break the streak`() {
        val completed = setOf(LocalDate(2026, 8, 3), LocalDate(2026, 8, 5), LocalDate(2026, 8, 7))
        val result = reconcile(
            weekdays("gym", completed, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            today = LocalDate(2026, 8, 8),
        )

        assertEquals(3, result.habits.single().streak)
        assertTrue(result.newEvents.none { it.type == ProtectionEventType.CONSUME })
    }

    @Test
    fun `weekly habit grants one protection after four successful weeks`() {
        val dates = (0 until 4).map { week ->
            start.plus(DatePeriod(days = week * 7))
        }.toSet()
        val result = reconcile(
            weekly("meditate", dates, daysPerWeek = 1),
            today = start.plus(DatePeriod(days = 28)),
        )

        assertEquals(1, result.balance)
        assertEquals(4, result.habits.single().streak)
        assertEquals(1, result.newEvents.count { it.type == ProtectionEventType.GRANT })
    }

    @Test
    fun `a missed occasion spends one protection and keeps the streak`() {
        val dates = (0 until 3).map { start.plus(DatePeriod(days = it)) }.toSet()
        val missed = reconcile(
            daily("read", dates),
            today = start.plus(DatePeriod(days = 4)),
            existing = listOf(ProtectionEvent("bank", "seed", ProtectionEventType.GRANT)),
        )

        assertEquals(0, missed.balance)
        assertEquals(3, missed.habits.single().streak)
        assertEquals(StreakOutcome.PROTECTED, missed.habits.single().outcome)
        assertEquals(1, missed.newEvents.count { it.type == ProtectionEventType.CONSUME })
    }

    @Test
    fun `a miss without protections breaks the streak`() {
        val dates = setOf(start, start.plus(DatePeriod(days = 1)))
        val result = reconcile(daily("read", dates), today = start.plus(DatePeriod(days = 4)))

        assertEquals(0, result.habits.single().streak)
        assertEquals(StreakOutcome.BROKEN, result.habits.single().outcome)
        assertEquals(0, result.balance)
    }

    @Test
    fun `inventory never passes three protections`() {
        val dates = (0 until 28).map { start.plus(DatePeriod(days = it)) }.toSet()
        val result = reconcile(daily("read", dates), today = start.plus(DatePeriod(days = 28)))

        assertEquals(MAX_STREAK_PROTECTIONS, result.balance)
        assertEquals(3, result.newEvents.count { it.type == ProtectionEventType.GRANT })
    }

    @Test
    fun `the same occasion cannot grant twice`() {
        val dates = (0 until 7).map { start.plus(DatePeriod(days = it)) }.toSet()
        val today = start.plus(DatePeriod(days = 7))
        val first = reconcile(daily("read", dates), today = today)
        val second = reconcile(daily("read", dates), today = today, existing = first.newEvents)

        assertTrue(second.newEvents.isEmpty())
        assertEquals(1, protectionBalance(first.newEvents + second.newEvents))
    }

    @Test
    fun `today and a future day do not spend a protection`() {
        val earned = ProtectionEvent("bank", "seed", ProtectionEventType.GRANT)
        val result = reconcile(
            daily("read", setOf(start)),
            today = start.plus(DatePeriod(days = 1)),
            existing = listOf(earned),
        )

        assertEquals(1, result.balance)
        assertTrue(result.newEvents.none { it.type == ProtectionEventType.CONSUME })
    }

    @Test
    fun `a protected miss does not advance the next protection`() {
        val dates = (0 until 6).map { start.plus(DatePeriod(days = it)) }.toSet()
        val result = reconcile(
            daily("read", dates),
            today = start.plus(DatePeriod(days = 7)),
            existing = listOf(ProtectionEvent("bank", "seed", ProtectionEventType.GRANT)),
        )

        assertEquals(6, result.habits.single().progress)
        assertTrue(result.newEvents.none { it.type == ProtectionEventType.GRANT })
    }

    @Test
    fun `two habits share one inventory`() {
        val yesterday = start
        val result = reconcileStreakProtections(
            habits = listOf(
                daily("a", emptySet()),
                daily("b", emptySet()),
            ),
            existingEvents = listOf(ProtectionEvent("bank", "seed", ProtectionEventType.GRANT)),
            today = yesterday.plus(DatePeriod(days = 1)),
            protectionSince = start,
        )

        assertEquals(0, result.balance)
        val protectedHabit = result.habits.single { it.outcome == StreakOutcome.PROTECTED }
        val brokenHabit = result.habits.single { it.outcome == StreakOutcome.BROKEN }
        assertEquals("a", protectedHabit.habitId)
        assertEquals("b", brokenHabit.habitId)
    }

    @Test
    fun `daylight saving instants on the same local date are one occasion`() {
        val zone = ZoneId.of("Europe/Madrid")
        val earlier = ZonedDateTime.of(2026, 10, 25, 2, 30, 0, 0, zone).withEarlierOffsetAtOverlap()
        val later = ZonedDateTime.of(2026, 10, 25, 2, 30, 0, 0, zone).withLaterOffsetAtOverlap()
        val day = LocalDate.parse(earlier.toLocalDate().toString())

        assertEquals(day, LocalDate.parse(later.toLocalDate().toString()))

        val today = day.plus(DatePeriod(days = 1))
        val first = reconcile(daily("read", setOf(day)), today = today)
        val second = reconcile(daily("read", setOf(day)), today = today, existing = first.newEvents)

        assertTrue(second.newEvents.isEmpty())
        assertEquals(first.habits.single().streak, second.habits.single().streak)
    }

    @Test
    fun `a schedule change does not replay or duplicate older history`() {
        val result = reconcileStreakProtections(
            habits = listOf(
                weekly("meditate", setOf(start), daysPerWeek = 5).copy(
                    baselineStreak = 4,
                    baselineProgress = 2,
                    evaluateFrom = start.plus(DatePeriod(days = 10)),
                ),
            ),
            existingEvents = listOf(
                ProtectionEvent("meditate", "week:$start", ProtectionEventType.GRANT),
            ),
            today = start.plus(DatePeriod(days = 10)),
            protectionSince = start.plus(DatePeriod(days = 10)),
        )

        assertTrue(result.newEvents.isEmpty())
        assertEquals(4, result.habits.single().streak)
        assertEquals(2, result.habits.single().progress)
        assertEquals(1, protectionBalance(listOf(ProtectionEvent("meditate", "week:$start", ProtectionEventType.GRANT))))
    }

    private fun reconcile(
        habit: HabitProtectionInput,
        today: LocalDate,
        existing: List<ProtectionEvent> = emptyList(),
    ) = reconcileStreakProtections(
        habits = listOf(habit),
        existingEvents = existing,
        today = today,
        protectionSince = start,
    )

    private fun daily(id: String, dates: Set<LocalDate>) = HabitProtectionInput(
        habitId = id,
        schedule = HabitSchedule.DAILY,
        daysOfWeek = DayOfWeek.entries.toSet(),
        daysPerWeek = 7,
        completedDates = dates,
        createdOn = start,
        evaluateFrom = start,
    )

    private fun weekdays(id: String, dates: Set<LocalDate>, days: Set<DayOfWeek>) = HabitProtectionInput(
        habitId = id,
        schedule = HabitSchedule.WEEKDAYS,
        daysOfWeek = days,
        daysPerWeek = 7,
        completedDates = dates,
        createdOn = start,
        evaluateFrom = start,
    )

    private fun weekly(id: String, dates: Set<LocalDate>, daysPerWeek: Int) = HabitProtectionInput(
        habitId = id,
        schedule = HabitSchedule.WEEKLY_COUNT,
        daysOfWeek = DayOfWeek.entries.toSet(),
        daysPerWeek = daysPerWeek,
        completedDates = dates,
        createdOn = start,
        evaluateFrom = start,
    )
}
