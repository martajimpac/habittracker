package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.FriendListItem
import com.marta.habittracker.domain.model.Profile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeFriendsRepositoryTest {

    private val sampleProfile = Profile(
        id = "friend-1",
        username = "alice",
        displayName = "Alice",
        avatarColor = "#6750A4",
    )

    private val sampleFriend = FriendListItem(
        profile = sampleProfile,
        bestStreak = 5,
        publicHabitsCount = 2,
        activeChallengeCount = 1,
    )

    @Test
    fun `getAcceptedFriends returns configured friends list`() = runTest {
        val friends = listOf(sampleFriend)
        val repository = FakeFriendsRepository(
            acceptedFriendsResult = DataResult.Success(friends),
        )

        val result = repository.getAcceptedFriends()

        assertTrue(result is DataResult.Success)
        assertEquals(friends, (result as DataResult.Success).data)
        assertEquals(1, repository.getAcceptedFriendsCalls)
    }
}
