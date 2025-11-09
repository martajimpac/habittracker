package com.aristidevs.habittracker.view.core.navigation

import kotlinx.serialization.Serializable

sealed class TabScreens{

    @Serializable
    data object TabHome: TabScreens()

    @Serializable
    data object TabAddContent: TabScreens()

    @Serializable
    data object TabProfile: TabScreens()
}