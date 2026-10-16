package com.marta.habittracker.presentation.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.usecase.WEEKLY_GOAL_TARGET_DAYS
import com.marta.habittracker.domain.usecase.WeeklyGoalDay
import com.marta.habittracker.domain.usecase.weeklyGoalProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class GoalsUiState(
    val completedDays: Int = 0,
    val targetDays: Int = WEEKLY_GOAL_TARGET_DAYS,
    val days: List<WeeklyGoalDay> = emptyList(),
)

@HiltViewModel
class GoalsViewModel(
    habitRepository: HabitRepository,
    private val today: () -> LocalDate,
) : ViewModel() {

    @Inject
    constructor(habitRepository: HabitRepository) : this(habitRepository, { LocalDate.now() })

    val uiState: StateFlow<GoalsUiState> = habitRepository.getAllHabitsWithRecords()
        .map { habits ->
            val progress = weeklyGoalProgress(habits, today())
            GoalsUiState(
                completedDays = progress.completedDays,
                targetDays = progress.targetDays,
                days = progress.days,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GoalsUiState(),
        )
}
