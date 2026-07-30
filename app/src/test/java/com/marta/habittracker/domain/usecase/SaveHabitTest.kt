package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
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

        val result = useCase(
            name = "Leer",
            description = "30 minutos",
            daysOfWeek = days,
            icon = "📚",
            colorHex = "#D97706",
            reminderTime = "09:00",
            id = habitId,
        )

        assertTrue(result is DataResult.Success)
        assertEquals(1, repository.insertCalls)
        val inserted = repository.insertedHabits.single()
        assertEquals(habitId, inserted.id)
        assertEquals("Leer", inserted.name)
        assertEquals("30 minutos", inserted.description)
        assertEquals(setOf(KotlinDayOfWeek.MONDAY, KotlinDayOfWeek.FRIDAY), inserted.daysOfWeek)
        assertEquals("📚", inserted.icon)
        assertEquals("#D97706", inserted.colorHex)
        assertEquals("09:00", inserted.reminderTime)
    }

    @Test
    fun `invoke returns client generated uuid when id not provided`() = runTest {
        val repository = FakeHabitRepository()
        val useCase = SaveHabit(repository)

        val result = useCase(
            name = "Correr",
            description = null,
            daysOfWeek = setOf(DayOfWeek.TUESDAY),
            icon = "directions_run",
            colorHex = "#059669",
            reminderTime = null,
        )

        val id = (result as DataResult.Success).data
        assertEquals(id, repository.insertedHabits.single().id)
        assertTrue(UUID.fromString(id).toString() == id)
    }

    @Test
    fun `invoke returns provided id`() = runTest {
        val repository = FakeHabitRepository()
        val useCase = SaveHabit(repository)
        val habitId = "22222222-2222-2222-2222-222222222222"

        val result = useCase(
            name = "Correr",
            description = null,
            daysOfWeek = setOf(DayOfWeek.TUESDAY),
            icon = "directions_run",
            colorHex = "#059669",
            reminderTime = "06:00",
            id = habitId,
        )

        assertEquals(habitId, (result as DataResult.Success).data)
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
    fun `invoke converts java daysOfWeek set to kotlinx set for domain`() = runTest {
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
            setOf(KotlinDayOfWeek.WEDNESDAY, KotlinDayOfWeek.MONDAY),
            repository.insertedHabits.single().daysOfWeek,
        )
    }
}
