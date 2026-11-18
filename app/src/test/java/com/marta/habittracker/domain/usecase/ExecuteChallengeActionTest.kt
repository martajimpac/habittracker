package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantAction
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeHabitDraft
import com.marta.habittracker.domain.model.ChallengeProposal
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.model.errors.AppError
import com.marta.habittracker.domain.model.errors.AssistantActionError
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Challenge branch of [ExecuteAssistantAction] (SPEC chat-challenges). */
class ExecuteChallengeActionTest {

    private val today = LocalDate(2026, 11, 16)
    private val laura = Profile(id = "laura-id", username = "laura", displayName = "Laura", avatarColor = "#FF7775")
    private val draft = ChallengeHabitDraft(
        name = "Meditar",
        icon = "menu_book",
        colorHex = "#294536",
        daysOfWeek = DayOfWeek.entries.toSet(),
        scheduleMode = "daily",
    )
    private val action = AssistantAction.CreateChallenge(
        challengeId = "challenge-id",
        habitId = "habit-id",
        friend = laura,
        proposal = ChallengeProposal("Laura", draft, ChallengeCriteria.Streak, 14),
    )

    private fun friends(vararg profiles: Profile) =
        DataResult.Success(profiles.map { FriendListItem(it, bestStreak = 0, publicHabitsCount = 0, activeChallengeCount = 0) })

    private fun execute(friends: FakeFriendsRepository): ExecuteAssistantAction {
        val habits = FakeHabitRepository()
        return ExecuteAssistantAction(habits, SaveHabit(habits), friends)
    }

    // CA-03, CA-04: same path as the Friends sheet, with the card's fixed ids.
    @Test
    fun `confirm creates the challenge with the proposal ids`() = runTest {
        val repository = FakeFriendsRepository(acceptedFriendsResult = friends(laura))

        val result = execute(repository)(action, today)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(1, repository.createChallengeCalls)
        assertEquals("laura-id", repository.lastChallengedId)
        assertEquals(draft, repository.lastDraft)
        assertEquals(ChallengeCriteria.Streak, repository.lastChallengeCriteria)
        assertEquals(14, repository.lastDurationDays)
        assertEquals("habit-id", repository.lastChallengeHabitId)
        assertEquals("challenge-id", repository.lastChallengeId)
    }

    // CA-11
    @Test
    fun `friend removed after the proposal gives an error and creates nothing`() = runTest {
        val repository = FakeFriendsRepository(acceptedFriendsResult = friends())

        val result = execute(repository)(action, today)

        assertEquals(DataResult.Error(AssistantActionError.FriendNotFound), result)
        assertEquals(0, repository.createChallengeCalls)
    }

    // CA-05
    @Test
    fun `network error is returned and nothing is created`() = runTest {
        val offline = FakeFriendsRepository(acceptedFriendsResult = DataResult.Error(AppError.Common.Network))
        assertEquals(DataResult.Error(AppError.Common.Network), execute(offline)(action, today))
        assertEquals(0, offline.createChallengeCalls)

        val failing = FakeFriendsRepository(acceptedFriendsResult = friends(laura))
        failing.createChallengeResult = DataResult.Error(AppError.Common.Network)
        assertEquals(DataResult.Error(AppError.Common.Network), execute(failing)(action, today))
    }
}
