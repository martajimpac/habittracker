package com.marta.habittracker.data.mapper

import com.marta.habittracker.data.remote.ai.dto.ChatActionBody
import com.marta.habittracker.data.remote.ai.dto.HabitCatalogBody
import com.marta.habittracker.data.remote.ai.dto.HabitScheduleBody
import com.marta.habittracker.data.remote.ai.dto.RecommendationHabitBody
import com.marta.habittracker.domain.model.AssistantAction
import com.marta.habittracker.domain.model.AssistantActionStatus
import com.marta.habittracker.domain.model.HabitCatalog
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.ProposedAction
import com.marta.habittracker.domain.model.storedValue
import com.marta.habittracker.domain.usecase.HabitWeekStat
import kotlinx.datetime.DayOfWeek

internal object AssistantActionMapper {

    private const val TYPE_CREATE = "create_habit"
    private const val TYPE_COMPLETION = "set_completion"

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
        else -> null
    }

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
    }

    fun AssistantActionStatus.wire(): String = when (this) {
        AssistantActionStatus.Pending -> "pending"
        AssistantActionStatus.Done -> "done"
        AssistantActionStatus.Cancelled -> "cancelled"
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
