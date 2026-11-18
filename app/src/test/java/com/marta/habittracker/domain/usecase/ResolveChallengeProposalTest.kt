package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeDiscardReason
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.HabitCatalog
import com.marta.habittracker.domain.model.Profile
import com.marta.habittracker.domain.model.ProposedAction
import com.marta.habittracker.domain.model.errors.AppError
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveChallengeProposalTest {

    private val catalog = HabitCatalog(icons = listOf("menu_book", "fitness_center"), colorHexes = listOf("#294536", "#FF7775"))
    private val laura = Profile(id = "laura-id", username = "laura", displayName = "Laura", avatarColor = "#FF7775")

    private fun friends(vararg profiles: Profile) =
        DataResult.Success(profiles.map { FriendListItem(it, bestStreak = 0, publicHabitsCount = 0, activeChallengeCount = 0) })

    private fun proposal(
        friendName: String = "Laura",
        name: String = "Meditar",
        scheduleMode: String? = "daily",
        days: List<String> = emptyList(),
        criteria: String? = null,
        durationDays: Int? = null,
        icon: String? = "fitness_center",
    ) = ProposedAction.CreateChallenge(
        friendName = friendName,
        name = name,
        icon = icon,
        color = "#ff7775",
        scheduleMode = scheduleMode,
        days = days,
        weeklyTarget = null,
        criteria = criteria,
        durationDays = durationDays,
    )

    private suspend fun resolve(proposal: ProposedAction.CreateChallenge, repository: FakeFriendsRepository) =
        ResolveChallengeProposal(repository)(proposal, catalog) { "new-id" }

    // CA-01
    @Test
    fun `single friend gives a card with the defaults`() = runTest {
        val result = resolve(proposal(), FakeFriendsRepository(acceptedFriendsResult = friends(laura)))

        val card = (result as ChallengeResolution.Card).action
        assertEquals(laura, card.friend)
        assertEquals("new-id", card.challengeId)
        assertEquals("new-id", card.habitId)
        assertEquals(ChallengeCriteria.CompletionPct, card.proposal.criteria)
        assertEquals(7, card.proposal.durationDays)
        assertEquals("Meditar", card.proposal.draft.name)
        assertEquals("#FF7775", card.proposal.draft.colorHex)
        assertEquals("daily", card.proposal.draft.scheduleMode)
        assertEquals("Laura", card.proposal.friendQuery)
    }

    // CA-02, CA-10
    @Test
    fun `explicit days criterion and rounded duration`() = runTest {
        val result = resolve(
            proposal(
                scheduleMode = "weekdays",
                days = listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
                criteria = "streak",
                durationDays = 10,
            ),
            FakeFriendsRepository(acceptedFriendsResult = friends(laura)),
        )

        val card = (result as ChallengeResolution.Card).action
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), card.proposal.draft.daysOfWeek)
        assertEquals("weekdays", card.proposal.draft.scheduleMode)
        assertEquals(ChallengeCriteria.Streak, card.proposal.criteria)
        assertEquals(7, card.proposal.durationDays)
    }

    // CA-07
    @Test
    fun `ambiguous name is discarded with the candidates`() = runTest {
        val other = laura.copy(id = "2", displayName = "Laura M.", username = "lauram")
        val result = resolve(proposal(friendName = "lau"), FakeFriendsRepository(acceptedFriendsResult = friends(laura, other)))

        val discard = (result as ChallengeResolution.Discarded).discard
        assertEquals(ChallengeDiscardReason.FriendAmbiguous, discard.reason)
        assertEquals(listOf("Laura", "Laura M."), discard.candidates)
    }

    // CA-07
    @Test
    fun `unknown friend is discarded`() = runTest {
        val result = resolve(proposal(friendName = "Pedro"), FakeFriendsRepository(acceptedFriendsResult = friends(laura)))
        assertEquals(ChallengeDiscardReason.FriendNotFound, (result as ChallengeResolution.Discarded).discard.reason)
    }

    // CA-08
    @Test
    fun `no friends is discarded`() = runTest {
        val result = resolve(proposal(), FakeFriendsRepository())
        assertEquals(ChallengeDiscardReason.NoFriends, (result as ChallengeResolution.Discarded).discard.reason)
    }

    @Test
    fun `friends lookup failure is discarded with the error`() = runTest {
        val result = resolve(
            proposal(),
            FakeFriendsRepository(acceptedFriendsResult = DataResult.Error(AppError.Common.Network)),
        ) as ChallengeResolution.Discarded

        assertEquals(ChallengeDiscardReason.LookupFailed, result.discard.reason)
        assertEquals(AppError.Common.Network, result.error)
    }

    @Test
    fun `invalid habit or criterion gives no card and does not load friends`() = runTest {
        val repository = FakeFriendsRepository(acceptedFriendsResult = friends(laura))
        assertEquals(ChallengeResolution.Invalid, resolve(proposal(name = "  "), repository))
        assertEquals(ChallengeResolution.Invalid, resolve(proposal(scheduleMode = "weekdays"), repository))
        assertEquals(ChallengeResolution.Invalid, resolve(proposal(criteria = "fastest"), repository))
        assertEquals(ChallengeResolution.Invalid, resolve(proposal(friendName = ""), repository))
        assertEquals(0, repository.getAcceptedFriendsCalls)
    }

    @Test
    fun `unknown icon falls back to the default`() = runTest {
        val result = resolve(proposal(icon = "rocket"), FakeFriendsRepository(acceptedFriendsResult = friends(laura)))
        assertTrue(result is ChallengeResolution.Card)
        assertEquals("menu_book", (result as ChallengeResolution.Card).action.proposal.draft.icon)
    }
}
