package com.marta.habittracker.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.util.UUID

class SaveHabitTest {

    @Test
    fun `invoke inserts habit with provided fields including visuals and reminder`() = runTest {
        val repository = FakeHabitRepository()
        val useCase = SaveHabit(repository)
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        val habitId = "11111111-1111-1111-1111-111111111111"

        useCase(
            name = "Leer",
            description = "30 minutos",
            daysOfWeek = days,
            icon = "📚",
            colorHex = "#D97706",
            reminderTime = "09:00",
            id = habitId,
        )

        assertEquals(1, repository.insertCalls)
        val inserted = repository.insertedHabits.single()
        assertEquals(habitId, inserted.id)
        assertEquals("Leer", inserted.name)
        assertEquals("30 minutos", inserted.description)
        assertEquals(days.toList(), inserted.daysOfWeek)
        assertEquals("📚", inserted.icon)
        assertEquals("#D97706", inserted.colorHex)
        assertEquals("09:00", inserted.reminderTime)
    }

    @Test
    fun `invoke returns client generated uuid when id not provided`() = runTest {
        val repository = FakeHabitRepository()
        val useCase = SaveHabit(repository)

        val id = useCase(
            name = "Correr",
            description = null,
            daysOfWeek = setOf(DayOfWeek.TUESDAY),
            icon = "🏃",
            colorHex = "#059669",
            reminderTime = null,
        )

        assertEquals(id, repository.insertedHabits.single().id)
        assertTrue(UUID.fromString(id).toString() == id)
    }

    @Test
    fun `invoke returns provided id`() = runTest {
        val repository = FakeHabitRepository()
        val useCase = SaveHabit(repository)
        val habitId = "22222222-2222-2222-2222-222222222222"

        val id = useCase(
            name = "Correr",
            description = null,
            daysOfWeek = setOf(DayOfWeek.TUESDAY),
            icon = "🏃",
            colorHex = "#059669",
            reminderTime = "06:00",
            id = habitId,
        )

        assertEquals(habitId, id)
    }

    @Test
    fun `invoke accepts null description and reminderTime`() = runTest {
        val repository = FakeHabitRepository()
        val useCase = SaveHabit(repository)

        useCase(
            name = "Meditar",
            description = null,
            daysOfWeek = setOf(DayOfWeek.SUNDAY),
            icon = "🧘",
            colorHex = "#6750A4",
            reminderTime = null,
            id = "33333333-3333-3333-3333-333333333333",
        )

        val inserted = repository.insertedHabits.single()
        assertEquals(null, inserted.description)
        assertEquals(null, inserted.reminderTime)
    }

    @Test
    fun `invoke converts daysOfWeek set to list for persistence`() = runTest {
        val repository = FakeHabitRepository()
        val useCase = SaveHabit(repository)
        val days = linkedSetOf(
            DayOfWeek.WEDNESDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.WEDNESDAY,
        )

        useCase(
            name = "Estudiar",
            description = "Kotlin",
            daysOfWeek = days,
            icon = "🎯",
            colorHex = "#2563EB",
            reminderTime = "20:00",
            id = "44444444-4444-4444-4444-444444444444",
        )

        assertEquals(
            listOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY),
            repository.insertedHabits.single().daysOfWeek,
        )
    }
}
