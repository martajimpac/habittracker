package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantAction
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitRecord
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.errors.AppError
import com.marta.habittracker.domain.model.errors.AssistantActionError
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecuteAssistantActionTest {

    private val today = LocalDate(2026, 11, 12)
    private val yesterday = LocalDate(2026, 11, 11)

    private fun execute(repository: FakeHabitRepository) = ExecuteAssistantAction(repository, SaveHabit(repository))

    // CA-03
    @Test
    fun `create saves a private habit without reminder and with the proposal id`() = runTest {
        val repository = FakeHabitRepository()
        val action = AssistantAction.CreateHabit(
            habitId = "fixed-id",
            name = "Gym",
            description = null,
            icon = "fitness_center",
            colorHex = "#FF7775",
            schedule = HabitSchedule.WEEKDAYS,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            daysPerWeek = 7,
        )

        val result = execute(repository)(action, today)

        assertEquals(DataResult.Success(Unit), result)
        val saved = repository.insertedHabits.single()
        assertEquals("fixed-id", saved.id)
        assertEquals("Gym", saved.name)
        assertEquals("weekdays", saved.scheduleMode)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), saved.daysOfWeek)
        assertFalse(saved.isPublic)
        assertFalse(saved.reminderEnabled)
        assertEquals(null, saved.reminderTime)
    }

    @Test
    fun `create returns the repository error`() = runTest {
        val repository = FakeHabitRepository().apply { insertResult = DataResult.Error(AppError.Common.Network) }
        val action = AssistantAction.CreateHabit("id", "Read", null, "menu_book", "#294536", HabitSchedule.DAILY, DayOfWeek.entries.toSet(), 7)

        assertEquals(DataResult.Error(AppError.Common.Network), execute(repository)(action, today))
    }

    // CA-11
    @Test
    fun `marking a past day saves it as done`() = runTest {
        val repository = FakeHabitRepository(allHabitsWithRecords = listOf(habit()))

        val result = execute(repository)(mark(yesterday, completed = true), today)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(listOf(Triple("h", false, java.time.LocalDate.of(2026, 11, 11))), repository.toggledHabits)
    }

    // CA-12
    @Test
    fun `removing a done mark saves it as not done`() = runTest {
        val repository = FakeHabitRepository(allHabitsWithRecords = listOf(habit(done = listOf(yesterday))))

        execute(repository)(mark(yesterday, completed = false), today)

        assertEquals(listOf(Triple("h", true, java.time.LocalDate.of(2026, 11, 11))), repository.toggledHabits)
    }

    // CA-17
    @Test
    fun `already done from Home stays done and nothing is written`() = runTest {
        val repository = FakeHabitRepository(allHabitsWithRecords = listOf(habit(done = listOf(today))))

        val result = execute(repository)(mark(today, completed = true), today)

        assertEquals(DataResult.Success(Unit), result)
        assertTrue(repository.toggledHabits.isEmpty())
    }

    // CA-18
    @Test
    fun `deleted habit gives an error and writes nothing`() = runTest {
        val repository = FakeHabitRepository(allHabitsWithRecords = emptyList())

        val result = execute(repository)(mark(yesterday, completed = true), today)

        assertEquals(DataResult.Error(AssistantActionError.HabitNotFound), result)
        assertTrue(repository.toggledHabits.isEmpty())
    }

    // Cambio de semana: la tarjeta del domingo se confirma el lunes.
    @Test
    fun `a day from last week gives an error`() = runTest {
        val repository = FakeHabitRepository(allHabitsWithRecords = listOf(habit()))
        val sunday = LocalDate(2026, 11, 15)
        val nextMonday = LocalDate(2026, 11, 16)

        val result = execute(repository)(mark(sunday, completed = true), nextMonday)

        assertEquals(DataResult.Error(AssistantActionError.DayNotAllowed), result)
        assertTrue(repository.toggledHabits.isEmpty())
    }

    @Test
    fun `network error is returned`() = runTest {
        val repository = FakeHabitRepository(allHabitsWithRecords = listOf(habit())).apply {
            toggleResult = DataResult.Error(AppError.Common.Network)
        }

        assertEquals(DataResult.Error(AppError.Common.Network), execute(repository)(mark(yesterday, true), today))
    }

    private fun mark(date: LocalDate, completed: Boolean) =
        AssistantAction.SetCompletion("h", "Read", "menu_book", "#294536", date, completed)

    private fun habit(done: List<LocalDate> = emptyList()) = Habit(
        id = "h",
        name = "Read",
        description = null,
        daysOfWeek = DayOfWeek.entries.toSet(),
        icon = "menu_book",
        colorHex = "#294536",
        reminderTime = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        records = done.mapIndexed { index, date -> HabitRecord("r$index", "h", date, isCompleted = true) },
        scheduleMode = "daily",
    )
}
