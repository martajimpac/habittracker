package com.marta.habittracker.presentation.navigation

import kotlinx.serialization.Serializable

sealed class TabScreens {

    @Serializable
    data object TabHome : TabScreens()

    @Serializable
    data object TabStats : TabScreens()

    @Serializable
    data object TabFriends : TabScreens()

    @Serializable
    data object TabAddContent : TabScreens()

    @Serializable
    data object TabProfile : TabScreens()

    @Serializable
    data object TabPreferences : TabScreens()

    @Serializable
    data object TabGoals : TabScreens()

    @Serializable
    data object TabDeleteAccount : TabScreens()

    @Serializable
    data class TabDetail(val habitId: String) : TabScreens()

    @Serializable
    data class TabEditHabit(val habitId: String) : TabScreens()

    @Serializable
    data class TabChallengeDetail(val challengeId: String) : TabScreens()
}
