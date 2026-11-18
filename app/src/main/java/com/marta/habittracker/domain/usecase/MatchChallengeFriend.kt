package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.Profile
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/** Challenge lengths the Friends sheet offers; the chat uses the same ones. */
val CHALLENGE_DURATIONS: List<Int> = listOf(3, 7, 14, 30)

const val DEFAULT_CHALLENGE_DURATION = 7

/** Shorter queries match too much (for example a single letter), so they count as not found. */
private const val MIN_FRIEND_QUERY_LENGTH = 2

/** Closest allowed length. On a tie, the longer one; below 3 is 3 and above 30 is 30. */
fun nearestChallengeDuration(days: Int): Int =
    CHALLENGE_DURATIONS.minWith(
        compareBy<Int> { abs(it - days) }.thenByDescending { it },
    )

sealed interface FriendMatch {
    data class Match(val friend: Profile) : FriendMatch
    data class Ambiguous(val candidates: List<Profile>) : FriendMatch
    data object NotFound : FriendMatch
    data object NoFriends : FriendMatch
}

/**
 * Finds the friend the person named. Compares with the display name and the username, ignoring
 * case, accents, a leading "@" and extra spaces. An exact match wins over partial ones; a partial
 * match is a name that starts with the query or has a word that starts with it.
 */
fun matchChallengeFriend(query: String, friends: List<Profile>): FriendMatch {
    if (friends.isEmpty()) return FriendMatch.NoFriends
    val wanted = normalizeName(query).removePrefix("@").trim()
    if (wanted.length < MIN_FRIEND_QUERY_LENGTH) return FriendMatch.NotFound

    val exact = friends.filter { friend ->
        normalizeName(friend.displayName) == wanted || normalizeName(friend.username) == wanted
    }
    val matches = exact.ifEmpty {
        friends.filter { friend ->
            val display = normalizeName(friend.displayName)
            display.startsWith(wanted) ||
                display.split(' ').any { it.startsWith(wanted) } ||
                normalizeName(friend.username).startsWith(wanted)
        }
    }
    return when (matches.size) {
        0 -> FriendMatch.NotFound
        1 -> FriendMatch.Match(matches.single())
        else -> FriendMatch.Ambiguous(matches.sortedBy { normalizeName(it.displayName) })
    }
}

private val MARKS = Regex("\\p{M}+")
private val SPACES = Regex("\\s+")

private fun normalizeName(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(MARKS, "")
        .lowercase(Locale.ROOT)
        .replace(SPACES, " ")
        .trim()
