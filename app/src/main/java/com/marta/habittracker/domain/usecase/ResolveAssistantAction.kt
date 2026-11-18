package com.marta.habittracker.domain.usecase

import com.marta.habittracker.domain.model.AssistantAction
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitCatalog
import com.marta.habittracker.domain.model.HabitSchedule
import com.marta.habittracker.domain.model.ProposedAction
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

const val CHAT_HABIT_NAME_MAX_LENGTH = 100

/**
 * Checks what Bloom proposed against the same rules as the habit form and Home. Returns the
 * action to show as a card, or null when no card should be shown (only Bloom's text).
 */
fun resolveAssistantAction(
    proposal: ProposedAction,
    habits: List<Habit>,
    today: LocalDate,
    catalog: HabitCatalog,
    newId: () -> String,
): AssistantAction? = when (proposal) {
    is ProposedAction.CreateHabit -> resolveCreate(proposal, catalog, newId)
    is ProposedAction.SetCompletion -> resolveCompletion(proposal, habits, today)
    // Needs the friends list (network): resolved by ResolveChallengeProposal.
    is ProposedAction.CreateChallenge -> null
}

private fun resolveCreate(
    proposal: ProposedAction.CreateHabit,
    catalog: HabitCatalog,
    newId: () -> String,
): AssistantAction.CreateHabit? {
    val fields = resolveHabitFields(
        name = proposal.name,
        scheduleMode = proposal.scheduleMode,
        days = proposal.days,
        weeklyTarget = proposal.weeklyTarget,
        icon = proposal.icon,
        color = proposal.color,
        catalog = catalog,
    ) ?: return null
    return AssistantAction.CreateHabit(
        habitId = newId(),
        name = fields.name,
        description = proposal.description?.trim()?.ifEmpty { null },
        icon = fields.icon,
        colorHex = fields.colorHex,
        schedule = fields.schedule,
        daysOfWeek = fields.daysOfWeek,
        daysPerWeek = fields.daysPerWeek,
    )
}

/** Habit fields checked with the habit form rules (shared by habits and challenges). */
internal data class ResolvedHabitFields(
    val name: String,
    val icon: String,
    val colorHex: String,
    val schedule: HabitSchedule,
    val daysOfWeek: Set<DayOfWeek>,
    val daysPerWeek: Int,
)

/** Null when the name or the frequency is not valid. Unknown icon or color: the defaults. */
internal fun resolveHabitFields(
    name: String,
    scheduleMode: String?,
    days: List<String>,
    weeklyTarget: Int?,
    icon: String?,
    color: String?,
    catalog: HabitCatalog,
): ResolvedHabitFields? {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || trimmed.length > CHAT_HABIT_NAME_MAX_LENGTH) return null

    val schedule = when (scheduleMode) {
        "daily" -> HabitSchedule.DAILY
        "weekdays" -> HabitSchedule.WEEKDAYS
        "weekly_count" -> HabitSchedule.WEEKLY_COUNT
        else -> return null
    }
    val daysOfWeek = when (schedule) {
        HabitSchedule.WEEKDAYS -> days
            .mapNotNull { day -> DayOfWeek.entries.find { it.name == day } }
            .toSet()
            .ifEmpty { return null }
        else -> DayOfWeek.entries.toSet()
    }
    val daysPerWeek = when (schedule) {
        HabitSchedule.WEEKLY_COUNT -> weeklyTarget?.takeIf { it in 1..7 } ?: return null
        else -> 7
    }

    return ResolvedHabitFields(
        name = trimmed,
        icon = icon?.takeIf { it in catalog.icons } ?: catalog.defaultIcon,
        colorHex = catalog.colorHexes.find { it.equals(color, ignoreCase = true) }
            ?: catalog.defaultColorHex,
        schedule = schedule,
        daysOfWeek = daysOfWeek,
        daysPerWeek = daysPerWeek,
    )
}

private fun resolveCompletion(
    proposal: ProposedAction.SetCompletion,
    habits: List<Habit>,
    today: LocalDate,
): AssistantAction.SetCompletion? {
    val habit = habits.find { it.id == proposal.habitId } ?: return null
    val date = runCatching { LocalDate.parse(proposal.date) }.getOrNull() ?: return null
    if (!isEditableDay(habit, date, today)) return null
    if (habit.isDoneOn(date) == proposal.completed) return null
    return AssistantAction.SetCompletion(
        habitId = habit.id,
        habitName = habit.name,
        habitIcon = habit.icon,
        habitColorHex = habit.colorHex,
        date = date,
        completed = proposal.completed,
    )
}

/** Same rule as Home: today or an earlier day of this week, on a day the habit appears. */
internal fun isEditableDay(habit: Habit, date: LocalDate, today: LocalDate): Boolean =
    isInEditableRange(date, today) && habit.shownOn(date)

/** From Monday of [today]'s week up to [today]. */
internal fun isInEditableRange(date: LocalDate, today: LocalDate): Boolean {
    val monday = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
    return date in monday..today
}

internal fun Habit.isDoneOn(date: LocalDate): Boolean =
    records.any { it.date == date && it.isCompleted }
