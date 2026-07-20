package com.marta.habittracker.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class SaveHabitTest {

    @Test
    fun `invoke inserts habit with provided fields`() = runTest {
        val repository = FakeHabitRepository()
        val useCase = SaveHabit(repository)
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)

        useCase(
            name = "Leer",
            description = "30 minutos",
            daysOfWeek = days,
        )

        assertEquals(1, repository.insertCalls)
        val inserted = repository.insertedHabits.single()
        assertEquals("Leer", inserted.name)
        assertEquals("30 minutos", inserted.description)
        assertEquals(days.toList(), inserted.daysOfWeek)
    }

    @Test
    fun `invoke returns id from repository`() = runTest {
        val repository = FakeHabitRepository(insertResult = 99L)
        val useCase = SaveHabit(repository)

        val id = useCase(
            name = "Correr",
            description = null,
            daysOfWeek = setOf(DayOfWeek.TUESDAY),
        )

        assertEquals(99L, id)
    }

    @Test
    fun `invoke accepts null description`() = runTest {
        val repository = FakeHabitRepository()
        val useCase = SaveHabit(repository)

        useCase(
            name = "Meditar",
            description = null,
            daysOfWeek = setOf(DayOfWeek.SUNDAY),
        )

        assertEquals(null, repository.insertedHabits.single().description)
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
        )

        assertEquals(
            listOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY),
            repository.insertedHabits.single().daysOfWeek,
        )
    }

    @Test
    fun `invoke persists habit even when daysOfWeek is empty`() = runTest {
        val repository = FakeHabitRepository()
        val useCase = SaveHabit(repository)

        val id = useCase(
            name = "Sin dias",
            description = "boundary",
            daysOfWeek = emptySet(),
        )

        assertEquals(1, repository.insertCalls)
        assertEquals(emptyList<DayOfWeek>(), repository.insertedHabits.single().daysOfWeek)
        assertEquals(42L, id)
    }
}
