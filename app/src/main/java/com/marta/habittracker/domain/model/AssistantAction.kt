package com.marta.habittracker.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** An action Bloom proposed, already checked against the app rules. Nothing is saved until confirmed. */
sealed interface AssistantAction {

    /**
     * New habit. [habitId] is chosen when the card is shown and reused on every retry, so a
     * retry after a lost response cannot create a second habit.
     */
    data class CreateHabit(
        val habitId: String,
        val name: String,
        val description: String?,
        val icon: String,
        val colorHex: String,
        val schedule: HabitSchedule,
        val daysOfWeek: Set<DayOfWeek>,
        val daysPerWeek: Int,
    ) : AssistantAction

    /** Leaves [date] done ([completed] = true) or not done for an existing habit. */
    data class SetCompletion(
        val habitId: String,
        val habitName: String,
        val habitIcon: String,
        val habitColorHex: String,
        val date: LocalDate,
        val completed: Boolean,
    ) : AssistantAction
}

enum class AssistantActionStatus {
    Pending,
    Done,
    Cancelled,
}

/** An action as Bloom sent it, before any check. Values are raw and may be invalid. */
sealed interface ProposedAction {

    data class CreateHabit(
        val name: String,
        val description: String?,
        val icon: String?,
        val color: String?,
        val scheduleMode: String?,
        val days: List<String>,
        val weeklyTarget: Int?,
    ) : ProposedAction

    data class SetCompletion(
        val habitId: String,
        val date: String,
        val completed: Boolean,
    ) : ProposedAction
}

/** Bloom's reply: the text and, optionally, a proposal. */
data class ChatReply(
    val text: String,
    val proposal: ProposedAction? = null,
)

/** Icons and colors the habit form offers. Bloom may only pick from these. */
data class HabitCatalog(
    val icons: List<String>,
    val colorHexes: List<String>,
) {
    val defaultIcon: String get() = icons.first()
    val defaultColorHex: String get() = colorHexes.first()
}
