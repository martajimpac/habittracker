package com.marta.habittracker.presentation.navigation

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchTargetCoordinatorTest {

    @Test
    fun `target opened before collection is delivered once`() = runTest {
        val coordinator = LaunchTargetCoordinator()
        coordinator.open(LaunchTarget.ChallengeDetail("c1"))

        assertEquals(LaunchTarget.ChallengeDetail("c1"), coordinator.events.first())

        val replayed = mutableListOf<LaunchTarget>()
        val job = launch { coordinator.events.collect { replayed.add(it) } }
        advanceUntilIdle()
        assertEquals(emptyList<LaunchTarget>(), replayed)
        job.cancel()
    }
}
