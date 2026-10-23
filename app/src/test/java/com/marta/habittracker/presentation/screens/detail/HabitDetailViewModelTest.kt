package com.marta.habittracker.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.repository.StreakProtectionRepository
import com.marta.habittracker.domain.usecase.FakeFriendsRepository
import com.marta.habittracker.domain.usecase.FakeHabitRepository
import com.marta.habittracker.domain.usecase.HabitProtectionStatus
import com.marta.habittracker.domain.usecase.ProtectionReconcileResult
import com.marta.habittracker.domain.usecase.StreakOutcome
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `detail exposes progress toward the next protection`() = runTest {
        val habit = sampleHabit(reminderTime = null)
        val viewModel = createViewModel(
            habit = habit,
            streakProtectionRepository = FakeStreakProtectionRepository(
                ProtectionReconcileResult(
                    balance = 0,
                    habits = listOf(
                        HabitProtectionStatus(
                            habitId = habit.id,
                            streak = 2,
                            bestStreak = 2,
                            progress = 2,
                            milestone = 4,
                            outcome = StreakOutcome.ONGOING,
                        ),
                    ),
                    newEvents = emptyList(),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.protectionProgress.value?.progress)
        assertEquals(4, viewModel.protectionProgress.value?.milestone)
    }

    @Test
    fun `load with reminder enabled shows the saved time`() = runTest {
        val viewModel = createViewModel(sampleHabit(reminderTime = "08:00", reminderEnabled = true))
        advanceUntilIdle()

        assertTrue(viewModel.reminderEnabled.value)
        assertEquals("08:00", viewModel.reminderTime.value)
    }

    @Test
    fun `load with null reminderTime disables reminder and drafts 08 00`() = runTest {
        val viewModel = createViewModel(sampleHabit(reminderTime = null))
        advanceUntilIdle()

        assertFalse(viewModel.reminderEnabled.value)
        assertEquals("08:00", viewModel.reminderTime.value)
    }

    @Test
    fun `save with reminder disabled keeps the time`() = runTest {
        val habit = sampleHabit(reminderTime = "08:00", reminderEnabled = true)
        val repository = FakeHabitRepository(habitsById = mapOf(habit.id to habit))
        val viewModel = HabitDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("habitId" to habit.id)),
            friendsRepository = FakeFriendsRepository(),
            streakProtectionRepository = FakeStreakProtectionRepository(),
        )
        advanceUntilIdle()

        viewModel.onReminderEnabled(false)
        viewModel.saveReminder()
        advanceUntilIdle()

        assertEquals("08:00", repository.updatedHabits.single().reminderTime)
        assertFalse(repository.updatedHabits.single().reminderEnabled)
        assertEquals(habit.id, repository.updatedHabits.single().id)
    }

    @Test
    fun `save with reminder enabled persists 07 30`() = runTest {
        val habit = sampleHabit(reminderTime = null)
        val repository = FakeHabitRepository(habitsById = mapOf(habit.id to habit))
        val viewModel = HabitDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("habitId" to habit.id)),
            friendsRepository = FakeFriendsRepository(),
            streakProtectionRepository = FakeStreakProtectionRepository(),
        )
        advanceUntilIdle()

        viewModel.onReminderEnabled(true)
        viewModel.onReminderTimeChanged("07:30")
        viewModel.saveReminder()
        advanceUntilIdle()

        assertEquals("07:30", repository.updatedHabits.single().reminderTime)
        assertTrue(repository.updatedHabits.single().reminderEnabled)
    }

    @Test
    fun `deleteHabit emits habitDeleted`() = runTest {
        val habit = sampleHabit(reminderTime = null)
        val repository = FakeHabitRepository(habitsById = mapOf(habit.id to habit))
        val viewModel = HabitDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("habitId" to habit.id)),
            friendsRepository = FakeFriendsRepository(),
            streakProtectionRepository = FakeStreakProtectionRepository(),
        )
        advanceUntilIdle()
        var deleted = false
        val collectJob = launch {
            viewModel.habitDeleted.collect { deleted = true }
        }

        viewModel.deleteHabit()
        advanceUntilIdle()

        assertTrue(deleted)
        collectJob.cancel()
    }

    @Test
    fun `reminder denied turns toggle off and sets actionErrorRes`() = runTest {
        val viewModel = createViewModel(sampleHabit(reminderTime = "08:00"))
        advanceUntilIdle()

        viewModel.onReminderEnabled(true)
        viewModel.onReminderDenied(R.string.habit_reminder_permission_denied)

        assertFalse(viewModel.reminderEnabled.value)
        assertEquals(
            R.string.habit_reminder_permission_denied,
            viewModel.actionErrorRes.value,
        )
    }

    @Test
    fun `habit in a challenge stays marked when the next load fails`() = runTest {
        val habit = sampleHabit(reminderTime = null)
        val friends = FakeFriendsRepository()
        friends.challengeHabitIdsResult = DataResult.Success(setOf(habit.id))
        val viewModel = createViewModel(habit, friends)
        advanceUntilIdle()
        assertTrue(viewModel.inChallenge.value)

        friends.challengeHabitIdsResult = DataResult.Error(AppError.Common.Network)
        viewModel.refreshChallengeMembership()
        advanceUntilIdle()

        assertTrue(viewModel.inChallenge.value)
    }

    private fun createViewModel(
        habit: Habit,
        friendsRepository: FakeFriendsRepository = FakeFriendsRepository(),
        streakProtectionRepository: StreakProtectionRepository = FakeStreakProtectionRepository(),
    ): HabitDetailViewModel {
        val repository = FakeHabitRepository(habitsById = mapOf(habit.id to habit))
        return HabitDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("habitId" to habit.id)),
            friendsRepository = friendsRepository,
            streakProtectionRepository = streakProtectionRepository,
        )
    }

    private fun sampleHabit(
        reminderTime: String?,
        reminderEnabled: Boolean = false,
    ) = Habit(
        id = "habit-1",
        name = "Read",
        description = null,
        daysOfWeek = setOf(DayOfWeek.MONDAY),
        icon = "book",
        colorHex = "#6750A4",
        reminderTime = reminderTime,
        reminderEnabled = reminderEnabled,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        records = emptyList(),
    )
}

private class FakeStreakProtectionRepository(
    private val result: ProtectionReconcileResult = ProtectionReconcileResult(0, emptyList(), emptyList()),
) : StreakProtectionRepository {
    override suspend fun reconcile(habits: List<Habit>, today: LocalDate) = result
}
