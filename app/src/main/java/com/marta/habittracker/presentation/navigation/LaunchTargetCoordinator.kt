package com.marta.habittracker.presentation.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands widget / notification launch targets from [com.marta.habittracker.MainActivity]
 * to the navigation layer as one-shot events, so a target is navigated to exactly once
 * and never replayed on recomposition or activity recreation.
 */
@Singleton
class LaunchTargetCoordinator @Inject constructor() {

    private val _events = Channel<LaunchTarget>(Channel.BUFFERED)
    val events: Flow<LaunchTarget> = _events.receiveAsFlow()

    fun open(target: LaunchTarget) {
        _events.trySend(target)
    }
}

sealed interface LaunchTarget {
    data class HabitDetail(val habitId: String) : LaunchTarget
    data class ChallengeDetail(val challengeId: String) : LaunchTarget
    data class Tab(val tab: TabScreens) : LaunchTarget
}

@HiltViewModel
class LaunchTargetViewModel @Inject constructor(
    coordinator: LaunchTargetCoordinator,
) : ViewModel() {
    val events: Flow<LaunchTarget> = coordinator.events
}
