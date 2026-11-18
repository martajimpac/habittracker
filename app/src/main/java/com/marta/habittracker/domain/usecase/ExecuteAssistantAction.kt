package com.marta.habittracker.domain.usecase

import com.marta.habittracker.core.toJava
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantAction
import com.marta.habittracker.domain.model.storedValue
import com.marta.habittracker.domain.model.errors.AppError
import com.marta.habittracker.domain.model.errors.AssistantActionError
import com.marta.habittracker.domain.repository.FriendsRepository
import com.marta.habittracker.domain.repository.HabitRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/**
 * Saves a confirmed chat action with the same online-first paths as the habit form and Home.
 * The action is checked again against the current habits and [today], because the habit may
 * have been deleted or the week may have changed since Bloom proposed it.
 */
class ExecuteAssistantAction @Inject constructor(
    private val habitRepository: HabitRepository,
    private val saveHabit: SaveHabit,
    private val friendsRepository: FriendsRepository,
) {
    suspend operator fun invoke(action: AssistantAction, today: LocalDate): DataResult<Unit, AppError> =
        when (action) {
            is AssistantAction.CreateHabit -> create(action)
            is AssistantAction.SetCompletion -> setCompletion(action, today)
            is AssistantAction.CreateChallenge -> createChallenge(action)
        }

    /** Same path as the Friends sheet, with fixed ids so a retry cannot duplicate the challenge. */
    private suspend fun createChallenge(action: AssistantAction.CreateChallenge): DataResult<Unit, AppError> {
        val friends = when (val result = friendsRepository.getAcceptedFriends()) {
            is DataResult.Success -> result.data
            is DataResult.Error -> return result
        }
        if (friends.none { it.profile.id == action.friend.id }) {
            return DataResult.Error(AssistantActionError.FriendNotFound)
        }
        return friendsRepository.createChallenge(
            challengedId = action.friend.id,
            draft = action.proposal.draft,
            criteria = action.proposal.criteria,
            durationDays = action.proposal.durationDays,
            habitId = action.habitId,
            challengeId = action.challengeId,
        )
    }

    private suspend fun create(action: AssistantAction.CreateHabit): DataResult<Unit, AppError> {
        val result = saveHabit(
            name = action.name,
            description = action.description,
            daysPerWeek = action.daysPerWeek,
            scheduleMode = action.schedule.storedValue(),
            daysOfWeek = action.daysOfWeek,
            icon = action.icon,
            colorHex = action.colorHex,
            reminderTime = null,
            reminderEnabled = false,
            isPublic = false,
            id = action.habitId,
        )
        return when (result) {
            is DataResult.Success -> DataResult.Success(Unit)
            is DataResult.Error -> DataResult.Error(result.error)
        }
    }

    private suspend fun setCompletion(
        action: AssistantAction.SetCompletion,
        today: LocalDate,
    ): DataResult<Unit, AppError> {
        val habit = habitRepository.getAllHabitsWithRecords().first().find { it.id == action.habitId }
            ?: return DataResult.Error(AssistantActionError.HabitNotFound)
        if (!isInEditableRange(action.date, today)) {
            return DataResult.Error(AssistantActionError.DayNotAllowed)
        }
        val current = habit.isDoneOn(action.date)
        // Already in the requested state (for example, changed from Home meanwhile): nothing to write.
        if (current == action.completed) return DataResult.Success(Unit)
        if (!isEditableDay(habit, action.date, today)) {
            return DataResult.Error(AssistantActionError.DayNotAllowed)
        }
        // toggleHabitCompletion saves the opposite of the state it receives.
        return habitRepository.toggleHabitCompletion(habit.id, current, action.date.toJava())
    }
}
