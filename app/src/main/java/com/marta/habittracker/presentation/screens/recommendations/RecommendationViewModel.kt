package com.marta.habittracker.presentation.screens.recommendations

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.R
import com.marta.habittracker.core.IdGenerator
import com.marta.habittracker.core.toKotlin
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AssistantActionStatus
import com.marta.habittracker.domain.model.AssistantRole
import com.marta.habittracker.domain.model.AssistantTurn
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.HabitCatalog
import com.marta.habittracker.domain.model.errors.RecommendationError
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.domain.repository.RecommendationRepository
import com.marta.habittracker.domain.usecase.ChatContext
import com.marta.habittracker.domain.usecase.ExecuteAssistantAction
import com.marta.habittracker.domain.usecase.habitWeekRecommendationStats
import com.marta.habittracker.domain.usecase.resolveAssistantAction
import com.marta.habittracker.presentation.components.HabitIconOptions
import com.marta.habittracker.presentation.utils.HabitColorOptions
import com.marta.habittracker.presentation.utils.toHex
import com.marta.habittracker.presentation.utils.toUserMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChatPhase {
    LoadingHabits,
    Ready,
    Sending,
}

data class ChatUiState(
    val phase: ChatPhase = ChatPhase.LoadingHabits,
    val messages: List<AssistantTurn> = emptyList(),
    val draft: String = "",
    @StringRes val errorRes: Int? = null,
    val hasHabits: Boolean = false,
    /** Indexes in [messages] whose card is being saved; their buttons do not respond. */
    val savingActions: Set<Int> = emptySet(),
)

sealed interface ChatEvent {
    data class ShowError(@StringRes val messageRes: Int) : ChatEvent
}

@HiltViewModel
class RecommendationViewModel(
    habitRepository: HabitRepository,
    private val recommendations: RecommendationRepository,
    private val executeAction: ExecuteAssistantAction,
    private val today: () -> LocalDate,
    private val language: () -> String,
    private val catalog: HabitCatalog = formCatalog(),
    private val newId: () -> String = IdGenerator::newId,
) : ViewModel() {

    @Inject
    constructor(
        habitRepository: HabitRepository,
        recommendations: RecommendationRepository,
        executeAction: ExecuteAssistantAction,
    ) : this(
        habitRepository,
        recommendations,
        executeAction,
        { LocalDate.now() },
        { appChatLanguage(Locale.getDefault().language) },
    )

    private var habits: List<Habit> = emptyList()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChatEvent>(Channel.BUFFERED)
    val events: Flow<ChatEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            habitRepository.getAllHabitsWithRecords().collect { latest ->
                habits = latest
                _uiState.update { current ->
                    current.copy(
                        phase = if (current.phase == ChatPhase.LoadingHabits) ChatPhase.Ready else current.phase,
                        hasHabits = latest.isNotEmpty(),
                    )
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
        if (current.phase != ChatPhase.Ready || text.isEmpty()) return
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
            last?.role != AssistantRole.User
        ) {
            return
        }
        _uiState.update { it.copy(phase = ChatPhase.Sending, errorRes = null) }
        dispatch(current.messages)
    }

    /** Saves the action of the card at [index]. Ignored while it is saving or once it is done or cancelled. */
    fun confirmAction(index: Int) {
        val current = _uiState.value
        val turn = current.messages.getOrNull(index) ?: return
        val action = turn.action ?: return
        if (turn.actionStatus != AssistantActionStatus.Pending || index in current.savingActions) return
        _uiState.update { it.copy(savingActions = it.savingActions + index) }
        viewModelScope.launch {
            try {
                when (val result = executeAction(action, today().toKotlin())) {
                    is DataResult.Success -> setActionStatus(index, AssistantActionStatus.Done)
                    is DataResult.Error -> {
                        Log.e(TAG, "Chat action failed: ${action::class.simpleName} ${result.error}")
                        _events.send(ChatEvent.ShowError(result.error.toUserMessageRes()))
                    }
                }
            } finally {
                _uiState.update { it.copy(savingActions = it.savingActions - index) }
            }
        }
    }

    /** Discards the proposal of the card at [index]. Nothing is saved. */
    fun cancelAction(index: Int) {
        val current = _uiState.value
        val turn = current.messages.getOrNull(index) ?: return
        if (turn.actionStatus != AssistantActionStatus.Pending || index in current.savingActions) return
        setActionStatus(index, AssistantActionStatus.Cancelled)
    }

    private fun setActionStatus(index: Int, status: AssistantActionStatus) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.mapIndexed { i, turn ->
                    if (i == index) turn.copy(actionStatus = status) else turn
                },
            )
        }
    }

    private fun dispatch(messages: List<AssistantTurn>) {
        val snapshot = habits
        val day = today().toKotlin()
        val context = ChatContext(
            habits = habitWeekRecommendationStats(snapshot, day),
            today = day,
            catalog = catalog,
        )
        val languageCode = language()
        viewModelScope.launch {
            when (val result = recommendations.send(context, languageCode, messages)) {
                is DataResult.Success -> {
                    val action = result.data.proposal?.let { proposal ->
                        resolveAssistantAction(proposal, snapshot, day, catalog, newId)
                    }
                    _uiState.update {
                        it.copy(
                            phase = ChatPhase.Ready,
                            messages = it.messages + AssistantTurn(
                                role = AssistantRole.Assistant,
                                text = result.data.text,
                                action = action,
                                actionStatus = action?.let { AssistantActionStatus.Pending },
                            ),
                            errorRes = null,
                        )
                    }
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

    companion object {
        private const val TAG = "RecommendationViewModel"
    }
}

internal fun appChatLanguage(language: String): String {
    return if (language.startsWith("es")) "es" else "en"
}

/** The icons and colors offered by the habit form, in the same order (the first ones are the defaults). */
internal fun formCatalog(): HabitCatalog = HabitCatalog(
    icons = HabitIconOptions,
    colorHexes = HabitColorOptions.map { it.toHex() },
)

@StringRes
private fun RecommendationError.toMessageRes(): Int = when (this) {
    RecommendationError.InvalidRequest -> R.string.recommendation_error_invalid
    RecommendationError.TimedOut -> R.string.recommendation_error_timeout
    RecommendationError.EmptyResponse -> R.string.recommendation_error_empty
    RecommendationError.SessionExpired -> R.string.recommendation_error_session
    RecommendationError.Unavailable,
    RecommendationError.MissingBaseUrl,
    -> R.string.recommendation_error_unavailable
}
