package com.marta.habittracker.data.mapper

import com.marta.habittracker.data.remote.ai.dto.ChatActionBody
import com.marta.habittracker.data.remote.ai.dto.HabitCatalogBody
import com.marta.habittracker.data.remote.ai.dto.HabitScheduleBody
import com.marta.habittracker.data.remote.ai.dto.RecommendationHabitBody
import com.marta.habittracker.domain.model.AssistantAction
import com.marta.habittracker.domain.model.AssistantActionStatus
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.ChallengeCriteria
import com.marta.habittracker.domain.model.ChallengeDiscardReason
import com.marta.habittracker.domain.model.ChallengeProposal
import com.marta.habittracker.domain.model.HabitCatalog
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.ProposedAction
import com.marta.habittracker.domain.model.storedValue
import com.marta.habittracker.domain.usecase.HabitWeekStat
import kotlinx.datetime.DayOfWeek

internal object AssistantActionMapper {

    private const val TYPE_CREATE = "create_habit"
    private const val TYPE_COMPLETION = "set_completion"
    private const val TYPE_CHALLENGE = "create_challenge"
    private const val DISCARD_INVALID = "invalid_proposal"

    fun HabitWeekStat.toBody(): RecommendationHabitBody = RecommendationHabitBody(
        name = name,
        completedDays = completedDays,
        totalDays = totalDays,
        id = id.ifBlank { null },
        schedule = id.takeIf { it.isNotBlank() }?.let { scheduleBody(schedule, days, weeklyTarget) },
        doneDates = doneDates.map { it.toString() },
    )

    fun HabitCatalog.toBody(): HabitCatalogBody = HabitCatalogBody(icons = icons, colors = colorHexes)

    /** Null when the type is unknown or a required field is missing. */
    fun ChatActionBody.toProposal(): ProposedAction? = when (type) {
        TYPE_CREATE -> ProposedAction.CreateHabit(
            name = name ?: return null,
            description = description,
            icon = icon,
            color = color,
            scheduleMode = schedule?.mode,
            days = schedule?.days.orEmpty(),
            weeklyTarget = schedule?.weeklyTarget,
        )
        TYPE_COMPLETION -> ProposedAction.SetCompletion(
            habitId = habitId ?: return null,
            date = date ?: return null,
            completed = completed ?: return null,
        )
        TYPE_CHALLENGE -> ProposedAction.CreateChallenge(
            friendName = friendName ?: return null,
            name = name ?: return null,
            icon = icon,
            color = color,
            scheduleMode = schedule?.mode,
            days = schedule?.days.orEmpty(),
            weeklyTarget = schedule?.weeklyTarget,
            criteria = criteria,
            durationDays = durationDays,
        )
        else -> null
    }

    /**
     * The proposal of a Bloom turn as sent back in the history, with its status. A discarded
     * challenge sends its proposal too, so Bloom knows why no card was shown. Friend data never
     * leaves the app: only the name the person wrote ([ChallengeProposal.friendQuery]).
     */
    fun AssistantTurn.historyAction(): ChatActionBody? = action?.toBody() ?: discard?.proposal?.toBody()

    fun AssistantTurn.historyStatus(): String? =
        if (action != null || discard != null || invalidProposal) actionStatus?.wire() else null

    /** Why the app showed no card. An invalid proposal is sent without its action (its data is not valid). */
    fun AssistantTurn.historyDiscardReason(): String? =
        discard?.reason?.wire() ?: DISCARD_INVALID.takeIf { invalidProposal }

    /** The action as sent back in the history, so Bloom knows what it proposed. */
    fun AssistantAction.toBody(): ChatActionBody = when (this) {
        is AssistantAction.CreateHabit -> ChatActionBody(
            type = TYPE_CREATE,
            name = name,
            description = description,
            icon = icon,
            color = colorHex,
            schedule = scheduleBody(schedule, daysOfWeek, daysPerWeek),
        )
        is AssistantAction.SetCompletion -> ChatActionBody(
            type = TYPE_COMPLETION,
            habitId = habitId,
            date = date.toString(),
            completed = completed,
        )
        is AssistantAction.CreateChallenge -> proposal.toBody()
    }

    private fun ChallengeProposal.toBody(): ChatActionBody {
        val schedule = when (draft.scheduleMode) {
            "weekdays" -> HabitSchedule.WEEKDAYS
            "weekly_count" -> HabitSchedule.WEEKLY_COUNT
            else -> HabitSchedule.DAILY
        }
        return ChatActionBody(
            type = TYPE_CHALLENGE,
            friendName = friendQuery,
            name = draft.name,
            icon = draft.icon,
            color = draft.colorHex,
            schedule = scheduleBody(schedule, draft.daysOfWeek, draft.daysPerWeek),
            criteria = criteria.wire(),
            durationDays = durationDays,
        )
    }

    private fun ChallengeCriteria.wire(): String = when (this) {
        ChallengeCriteria.CompletionPct -> "completion_pct"
        ChallengeCriteria.AllDays -> "all_days"
        ChallengeCriteria.Streak -> "streak"
    }

    private fun ChallengeDiscardReason.wire(): String = when (this) {
        ChallengeDiscardReason.FriendNotFound -> "friend_not_found"
        ChallengeDiscardReason.FriendAmbiguous -> "friend_ambiguous"
        ChallengeDiscardReason.NoFriends -> "no_friends"
        ChallengeDiscardReason.LookupFailed -> "lookup_failed"
    }

    fun AssistantActionStatus.wire(): String = when (this) {
        AssistantActionStatus.Pending -> "pending"
        AssistantActionStatus.Done -> "done"
        AssistantActionStatus.Cancelled -> "cancelled"
        AssistantActionStatus.Discarded -> "discarded"
    }

    private fun scheduleBody(
        schedule: HabitSchedule,
        days: Set<DayOfWeek>,
        weeklyTarget: Int,
    ): HabitScheduleBody = HabitScheduleBody(
        mode = schedule.storedValue(),
        days = if (schedule == HabitSchedule.WEEKDAYS) days.sortedBy { it.ordinal }.map { it.name } else emptyList(),
        weeklyTarget = if (schedule == HabitSchedule.WEEKLY_COUNT) weeklyTarget.coerceIn(1, 7) else null,
    )
}
