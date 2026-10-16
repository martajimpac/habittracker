package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.model.ProfileError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateDisplayNameTest {

    @Test
    fun `blank name is invalid without calling repository`() = runTest {
        val repository = FakeProfileRepository()
        val useCase = UpdateDisplayName(repository)

        val result = useCase("   ")

        assertEquals(DataResult.Error(ProfileError.InvalidDisplayName), result)
        assertEquals(0, repository.updateDisplayNameCalls)
    }

    @Test
    fun `empty name is invalid without calling repository`() = runTest {
        val repository = FakeProfileRepository()
        val useCase = UpdateDisplayName(repository)

        val result = useCase("")

        assertEquals(DataResult.Error(ProfileError.InvalidDisplayName), result)
        assertEquals(0, repository.updateDisplayNameCalls)
    }

    @Test
    fun `name longer than 40 characters is invalid without calling repository`() = runTest {
        val repository = FakeProfileRepository()
        val useCase = UpdateDisplayName(repository)

        val result = useCase("a".repeat(41))

        assertEquals(DataResult.Error(ProfileError.InvalidDisplayName), result)
        assertEquals(0, repository.updateDisplayNameCalls)
    }

    @Test
    fun `valid name trims and updates repository`() = runTest {
        val updated = sampleProfile.copy(displayName = "Marta")
        val repository = FakeProfileRepository(
            updateDisplayNameResult = DataResult.Success(updated),
        )
        val useCase = UpdateDisplayName(repository)

        val result = useCase("  Marta  ")

        assertTrue(result is DataResult.Success)
        assertEquals(updated, (result as DataResult.Success).data)
        assertEquals(1, repository.updateDisplayNameCalls)
        assertEquals("Marta", repository.lastUpdatedDisplayName)
    }

    @Test
    fun `name of 40 characters is valid`() = runTest {
        val name = "a".repeat(40)
        val repository = FakeProfileRepository()
        val useCase = UpdateDisplayName(repository)

        val result = useCase(name)

        assertTrue(result is DataResult.Success)
        assertEquals(1, repository.updateDisplayNameCalls)
        assertEquals(name, repository.lastUpdatedDisplayName)
    }

    @Test
    fun `repository network error is returned for valid name`() = runTest {
        val repository = FakeProfileRepository(
            updateDisplayNameResult = DataResult.Error(AppError.Common.Network),
        )
        val useCase = UpdateDisplayName(repository)

        val result = useCase("Marta")

        assertEquals(DataResult.Error(AppError.Common.Network), result)
        assertEquals(1, repository.updateDisplayNameCalls)
    }

    companion object {
        private val sampleProfile = Profile(
            id = "user-1",
            username = "marta",
            displayName = "Marta",
            avatarColor = "#6750A4",
        )
    }
}
