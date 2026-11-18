package com.marta.habittracker.data.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.errors.AppError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateChallengeStepsTest {

    private class Remote(
        var habitInsert: DataResult<Unit, AppError> = DataResult.Success(Unit),
        var challengeInsert: DataResult<Unit, AppError> = DataResult.Success(Unit),
        var exists: DataResult<Boolean, AppError> = DataResult.Success(false),
    ) {
        var rollbacks = 0
        var challengeInserts = 0

        suspend fun run() = createChallengeSteps(
            insertHabit = { habitInsert },
            insertChallenge = { challengeInserts++; challengeInsert },
            challengeExists = { exists },
            rollbackHabit = { rollbacks++ },
        )
    }

    @Test
    fun `happy path creates once without rollback`() = runTest {
        val remote = Remote()
        assertEquals(DataResult.Success(Unit), remote.run())
        assertEquals(1, remote.challengeInserts)
        assertEquals(0, remote.rollbacks)
    }

    @Test
    fun `insert fails but challenge exists counts as success and keeps the habit`() = runTest {
        val remote = Remote(
            challengeInsert = DataResult.Error(AppError.Common.Unknown),
            exists = DataResult.Success(true),
        )
        assertEquals(DataResult.Success(Unit), remote.run())
        assertEquals(0, remote.rollbacks)
    }

    @Test
    fun `insert fails and challenge missing rolls back the habit and returns the error`() = runTest {
        val remote = Remote(challengeInsert = DataResult.Error(AppError.Common.Network))
        assertEquals(DataResult.Error(AppError.Common.Network), remote.run())
        assertEquals(1, remote.rollbacks)
    }

    @Test
    fun `existence check failing rolls back like before`() = runTest {
        val remote = Remote(
            challengeInsert = DataResult.Error(AppError.Common.Unknown),
            exists = DataResult.Error(AppError.Common.Network),
        )
        assertTrue(remote.run() is DataResult.Error)
        assertEquals(1, remote.rollbacks)
    }

    @Test
    fun `habit insert failing never touches the challenge`() = runTest {
        val remote = Remote(habitInsert = DataResult.Error(AppError.Common.Network))
        assertEquals(DataResult.Error(AppError.Common.Network), remote.run())
        assertEquals(0, remote.challengeInserts)
        assertEquals(0, remote.rollbacks)
    }
}
