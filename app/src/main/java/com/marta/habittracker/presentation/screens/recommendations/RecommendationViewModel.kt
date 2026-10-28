package com.marta.habittracker.presentation.screens.recommendations

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.core.toKotlin
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.RecommendationError
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.RecommendationRepository
import com.marta.habittracker.domain.usecase.HabitWeekStat
import com.marta.habittracker.domain.usecase.habitWeekRecommendationStats
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class RecommendationPhase {
    LoadingHabits,
    Idle,
    Loading,
    Empty,
    Ready,
    Error,
}

data class RecommendationUiState(
    val phase: RecommendationPhase = RecommendationPhase.LoadingHabits,
    val stats: List<HabitWeekStat> = emptyList(),
    val suggestion: String? = null,
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class RecommendationViewModel(
    habitRepository: HabitRepository,
    private val recommendations: RecommendationRepository,
    private val today: () -> LocalDate,
) : ViewModel() {

    @Inject
    constructor(
        habitRepository: HabitRepository,
        recommendations: RecommendationRepository,
    ) : this(habitRepository, recommendations, { LocalDate.now() })

    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            habitRepository.getAllHabitsWithRecords().collect { habits ->
                val stats = habitWeekRecommendationStats(habits, today().toKotlin())
                _uiState.update { current ->
                    when {
                        stats.isEmpty() -> current.copy(
                            phase = RecommendationPhase.Empty,
                            stats = emptyList(),
                            suggestion = null,
                            errorRes = null,
                        )
                        current.phase == RecommendationPhase.Loading ||
                            current.phase == RecommendationPhase.Ready ||
                            current.phase == RecommendationPhase.Error -> current.copy(stats = stats)
                        else -> current.copy(
                            phase = RecommendationPhase.Idle,
                            stats = stats,
                            errorRes = null,
                        )
                    }
                }
            }
        }
    }

    fun request() {
        val current = _uiState.value
        if (
            current.phase == RecommendationPhase.Loading ||
            current.phase == RecommendationPhase.Empty ||
            current.phase == RecommendationPhase.LoadingHabits ||
            current.stats.isEmpty()
        ) {
            return
        }
        _uiState.update {
            it.copy(phase = RecommendationPhase.Loading, suggestion = null, errorRes = null)
        }
        val stats = _uiState.value.stats
        viewModelScope.launch {
            when (val result = recommendations.request(stats)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        phase = RecommendationPhase.Ready,
                        suggestion = result.data,
                        errorRes = null,
                    )
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(
                        phase = RecommendationPhase.Error,
                        suggestion = null,
                        errorRes = result.error.toMessageRes(),
                    )
                }
            }
        }
    }
}

@StringRes
private fun RecommendationError.toMessageRes(): Int = when (this) {
    RecommendationError.InvalidRequest -> R.string.recommendation_error_invalid
    RecommendationError.TimedOut -> R.string.recommendation_error_timeout
    RecommendationError.EmptyResponse -> R.string.recommendation_error_empty
    RecommendationError.Unavailable,
    RecommendationError.MissingBaseUrl,
    -> R.string.recommendation_error_unavailable
}
