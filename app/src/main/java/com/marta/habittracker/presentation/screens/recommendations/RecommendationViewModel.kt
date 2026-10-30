package com.marta.habittracker.presentation.screens.recommendations

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.core.toKotlin
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantRole
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.RecommendationError
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.RecommendationRepository
import com.marta.habittracker.domain.usecase.HabitWeekStat
import com.marta.habittracker.domain.usecase.habitWeekRecommendationStats
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChatPhase {
    LoadingHabits,
    Empty,
    Ready,
    Sending,
}

data class ChatUiState(
    val phase: ChatPhase = ChatPhase.LoadingHabits,
    val messages: List<AssistantTurn> = emptyList(),
    val draft: String = "",
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class RecommendationViewModel(
    habitRepository: HabitRepository,
    private val recommendations: RecommendationRepository,
    private val today: () -> LocalDate,
    private val language: () -> String,
) : ViewModel() {

    @Inject
    constructor(
        habitRepository: HabitRepository,
        recommendations: RecommendationRepository,
    ) : this(
        habitRepository,
        recommendations,
        { LocalDate.now() },
        { appChatLanguage(Locale.getDefault().language) },
    )

    private var stats: List<HabitWeekStat> = emptyList()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            habitRepository.getAllHabitsWithRecords().collect { habits ->
                stats = habitWeekRecommendationStats(habits, today().toKotlin())
                _uiState.update { current ->
                    when {
                        stats.isEmpty() -> current.copy(
                            phase = ChatPhase.Empty,
                            errorRes = null,
                        )
                        current.phase == ChatPhase.Sending -> current
                        current.phase == ChatPhase.LoadingHabits ||
                            current.phase == ChatPhase.Empty -> current.copy(phase = ChatPhase.Ready)
                        else -> current
                    }
                }
            }
        }
    }

    fun onDraftChange(value: String) {
        _uiState.update { it.copy(draft = value) }
    }

    fun send() {
        val current = _uiState.value
        val text = current.draft.trim()
        if (current.phase != ChatPhase.Ready || text.isEmpty() || stats.isEmpty()) return
        val next = current.messages + AssistantTurn(role = AssistantRole.User, text = text)
        _uiState.update {
            it.copy(
                phase = ChatPhase.Sending,
                messages = next,
                draft = "",
                errorRes = null,
            )
        }
        dispatch(next)
    }

    fun retry() {
        val current = _uiState.value
        val last = current.messages.lastOrNull()
        if (
            current.phase != ChatPhase.Ready ||
            current.errorRes == null ||
            last?.role != AssistantRole.User ||
            stats.isEmpty()
        ) {
            return
        }
        _uiState.update { it.copy(phase = ChatPhase.Sending, errorRes = null) }
        dispatch(current.messages)
    }

    private fun dispatch(messages: List<AssistantTurn>) {
        val snapshot = stats
        val languageCode = language()
        viewModelScope.launch {
            when (val result = recommendations.send(snapshot, languageCode, messages)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        phase = ChatPhase.Ready,
                        messages = it.messages + AssistantTurn(
                            role = AssistantRole.Assistant,
                            text = result.data,
                        ),
                        errorRes = null,
                    )
                }
                is DataResult.Error -> _uiState.update {
                    it.copy(
                        phase = ChatPhase.Ready,
                        errorRes = result.error.toMessageRes(),
                    )
                }
            }
        }
    }
}

internal fun appChatLanguage(language: String): String {
    return if (language.startsWith("es")) "es" else "en"
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
