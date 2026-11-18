package com.marta.habittracker.domain.model.errors

/** Why a confirmed chat action could not be saved, besides network or server errors. */
sealed interface AssistantActionError : AppError {
    /** The habit was deleted after Bloom proposed the action. */
    data object HabitNotFound : AssistantActionError

    /** The day is no longer allowed: future, outside this week, or the habit is not due that day. */
    data object DayNotAllowed : AssistantActionError

    /** The person is no longer an accepted friend. */
    data object FriendNotFound : AssistantActionError
}
