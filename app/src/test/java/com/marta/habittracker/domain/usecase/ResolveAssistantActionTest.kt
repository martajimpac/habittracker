package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.AssistantAction
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitCatalog
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.ProposedAction
import java.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveAssistantActionTest {

    // Thursday; the week runs Monday 9 to Sunday 15.
    private val today = LocalDate(2026, 11, 12)
    private val catalog = HabitCatalog(icons = listOf("water_drop", "menu_book"), colorHexes = listOf("#294536", "#FF7775"))

    private fun resolve(proposal: ProposedAction, habits: List<Habit> = emptyList()) =
        resolveAssistantAction(proposal, habits, today, catalog) { "new-id" }

    private fun create(
        name: String = "Read",
        icon: String? = "menu_book",
        color: String? = "#ff7775",
        mode: String? = "daily",
        days: List<String> = emptyList(),
        target: Int? = null,
    ) = ProposedAction.CreateHabit(name, " Ten pages ", icon, color, mode, days, target)

    @Test
    fun `valid create proposal keeps its values and gets an id`() {
        val action = resolve(create()) as AssistantAction.CreateHabit
        assertEquals(
            AssistantAction.CreateHabit(
                habitId = "new-id",
                name = "Read",
                description = "Ten pages",
                icon = "menu_book",
                colorHex = "#FF7775",
                schedule = HabitSchedule.DAILY,
                daysOfWeek = DayOfWeek.entries.toSet(),
                daysPerWeek = 7,
            ),
            action,
        )
    }

    @Test
    fun `specific days and weekly target are kept`() {
        val gym = resolve(create(mode = "weekdays", days = listOf("MONDAY", "WEDNESDAY", "FRIDAY"))) as AssistantAction.CreateHabit
        assertEquals(HabitSchedule.WEEKDAYS, gym.schedule)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), gym.daysOfWeek)

        val weekly = resolve(create(mode = "weekly_count", target = 3)) as AssistantAction.CreateHabit
        assertEquals(HabitSchedule.WEEKLY_COUNT, weekly.schedule)
        assertEquals(3, weekly.daysPerWeek)
    }

    // CA-23
    @Test
    fun `unknown icon or color falls back to the defaults`() {
        val action = resolve(create(icon = "rocket", color = "#123456")) as AssistantAction.CreateHabit
        assertEquals("water_drop", action.icon)
        assertEquals("#294536", action.colorHex)
    }

    // CA-23
    @Test
    fun `invalid create proposals show no card`() {
        assertNull(resolve(create(name = "  ")))
        assertNull(resolve(create(name = "x".repeat(101))))
        assertNull(resolve(create(mode = "monthly")))
        assertNull(resolve(create(mode = "weekdays", days = listOf("FUNDAY"))))
        assertNull(resolve(create(mode = "weekly_count", target = 0)))
        assertNull(resolve(create(mode = "weekly_count", target = null)))
    }

    // CA-10
    @Test
    fun `marking yesterday is proposed`() {
        val habit = habit()
        val action = resolve(ProposedAction.SetCompletion("h", "2026-11-11", true), listOf(habit))
        assertEquals(
            AssistantAction.SetCompletion("h", "Read", "menu_book", "#294536", LocalDate(2026, 11, 11), true),
            action,
        )
    }

    // CA-12
    @Test
    fun `removing a done mark is proposed`() {
        val habit = habit(done = listOf(LocalDate(2026, 11, 9)))
        val action = resolve(ProposedAction.SetCompletion("h", "2026-11-09", false), listOf(habit))
        assertEquals(false, (action as AssistantAction.SetCompletion).completed)
    }

    // CA-13, CA-15, CA-23
    @Test
    fun `future, other week, unknown habit or bad date show no card`() {
        val habits = listOf(habit())
        assertNull(resolve(ProposedAction.SetCompletion("h", "2026-11-13", true), habits))
        assertNull(resolve(ProposedAction.SetCompletion("h", "2026-11-08", true), habits))
        assertNull(resolve(ProposedAction.SetCompletion("other", "2026-11-11", true), habits))
        assertNull(resolve(ProposedAction.SetCompletion("h", "yesterday", true), habits))
    }

    // CA-14
    @Test
    fun `a day the habit is not due shows no card`() {
        val habit = habit(days = setOf(DayOfWeek.MONDAY), mode = "weekdays")
        assertNull(resolve(ProposedAction.SetCompletion("h", "2026-11-12", true), listOf(habit)))
    }

    @Test
    fun `a weekly habit that reached its target cannot get another day`() {
        val habit = habit(mode = "weekly_count", daysPerWeek = 1, done = listOf(LocalDate(2026, 11, 9)))
        assertNull(resolve(ProposedAction.SetCompletion("h", "2026-11-12", true), listOf(habit)))
    }

    // CA-16
    @Test
    fun `already in the requested state shows no card`() {
        val habit = habit(done = listOf(today))
        assertNull(resolve(ProposedAction.SetCompletion("h", "2026-11-12", true), listOf(habit)))
    }

    private fun habit(
        mode: String = "daily",
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        daysPerWeek: Int = 7,
        done: List<LocalDate> = emptyList(),
    ) = Habit(
        id = "h",
        name = "Read",
        description = null,
        daysOfWeek = days,
        icon = "menu_book",
        colorHex = "#294536",
        reminderTime = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        records = done.mapIndexed { index, date -> HabitRecord("r$index", "h", date, isCompleted = true) },
        daysPerWeek = daysPerWeek,
        scheduleMode = mode,
    )
}
