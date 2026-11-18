package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantAction
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeDiscard
import com.marta.habittracker.domain.model.ChallengeDiscardReason
import com.marta.habittracker.domain.model.ChallengeHabitDraft
import com.marta.habittracker.domain.model.ChallengeProposal
import com.marta.habittracker.domain.model.HabitCatalog
import com.marta.habittracker.domain.model.ProposedAction
import com.marta.habittracker.domain.model.errors.AppError
import com.marta.habittracker.domain.model.storedValue
import com.marta.habittracker.domain.repository.FriendsRepository
import javax.inject.Inject

sealed interface ChallengeResolution {
    /** Show the card. */
    data class Card(val action: AssistantAction.CreateChallenge) : ChallengeResolution

    /** Show the app's notice instead of the card. [error] is set when the friends could not be loaded. */
    data class Discarded(val discard: ChallengeDiscard, val error: AppError? = null) : ChallengeResolution

    /** The proposal itself is not valid: only Bloom's text, as with other actions. */
    data object Invalid : ChallengeResolution
}

/**
 * Checks a challenge proposal with the same rules as the Friends sheet and looks the friend up by
 * the name the person wrote. Bloom never sees the friends list (SPEC chat-challenges, RF-06).
 */
class ResolveChallengeProposal @Inject constructor(
    private val friendsRepository: FriendsRepository,
) {
    suspend operator fun invoke(
        proposal: ProposedAction.CreateChallenge,
        catalog: HabitCatalog,
        newId: () -> String,
    ): ChallengeResolution {
        val checked = checkChallengeProposal(proposal, catalog) ?: return ChallengeResolution.Invalid

        val friends = when (val result = friendsRepository.getAcceptedFriends()) {
            is DataResult.Success -> result.data.map { it.profile }
            is DataResult.Error -> return ChallengeResolution.Discarded(
                discard = ChallengeDiscard(checked, ChallengeDiscardReason.LookupFailed),
                error = result.error,
            )
        }

        return when (val match = matchChallengeFriend(checked.friendQuery, friends)) {
            is FriendMatch.Match -> ChallengeResolution.Card(
                AssistantAction.CreateChallenge(
                    challengeId = newId(),
                    habitId = newId(),
                    friend = match.friend,
                    proposal = checked,
                ),
            )
            is FriendMatch.Ambiguous -> ChallengeResolution.Discarded(
                ChallengeDiscard(
                    proposal = checked,
                    reason = ChallengeDiscardReason.FriendAmbiguous,
                    candidates = match.candidates.map { it.displayName },
                ),
            )
            FriendMatch.NotFound -> ChallengeResolution.Discarded(
                ChallengeDiscard(checked, ChallengeDiscardReason.FriendNotFound),
            )
            FriendMatch.NoFriends -> ChallengeResolution.Discarded(
                ChallengeDiscard(checked, ChallengeDiscardReason.NoFriends),
            )
        }
    }
}

/** Form rules for the habit, criterion and length. Null when the proposal cannot be shown. */
internal fun checkChallengeProposal(
    proposal: ProposedAction.CreateChallenge,
    catalog: HabitCatalog,
): ChallengeProposal? {
    val friendQuery = proposal.friendName.trim()
    if (friendQuery.isEmpty() || friendQuery.length > CHAT_HABIT_NAME_MAX_LENGTH) return null
    val fields = resolveHabitFields(
        name = proposal.name,
        scheduleMode = proposal.scheduleMode,
        days = proposal.days,
        weeklyTarget = proposal.weeklyTarget,
        icon = proposal.icon,
        color = proposal.color,
        catalog = catalog,
    ) ?: return null
    val criteria = when (proposal.criteria) {
        null, "completion_pct" -> ChallengeCriteria.CompletionPct
        "all_days" -> ChallengeCriteria.AllDays
        "streak" -> ChallengeCriteria.Streak
        else -> return null
    }
    return ChallengeProposal(
        friendQuery = friendQuery,
        draft = ChallengeHabitDraft(
            name = fields.name,
            icon = fields.icon,
            colorHex = fields.colorHex,
            daysOfWeek = fields.daysOfWeek,
            daysPerWeek = fields.daysPerWeek,
            scheduleMode = fields.schedule.storedValue(),
        ),
        criteria = criteria,
        durationDays = nearestChallengeDuration(proposal.durationDays ?: DEFAULT_CHALLENGE_DURATION),
    )
}
