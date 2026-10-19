package com.marta.habittracker.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.marta.habittracker.R

sealed class BottomNavigation(
    @StringRes val label: Int,
    val icon: ImageVector,
    val tabScreen: TabScreens,
) {
    companion object {
        val destinationTabs = listOf(TabHome, TabStats, TabFriends, TabProfile)
        val tabBarItems = listOf(TabHome, TabStats, TabNew, TabFriends, TabProfile)
    }

    data object TabHome : BottomNavigation(
        label = R.string.tab_home,
        icon = Icons.Rounded.Home,
        tabScreen = TabScreens.TabHome,
    )

    data object TabStats : BottomNavigation(
        label = R.string.tab_stats,
        icon = Icons.AutoMirrored.Rounded.ShowChart,
        tabScreen = TabScreens.TabStats,
    )

    data object TabNew : BottomNavigation(
        label = R.string.tab_new,
        icon = Icons.Rounded.Add,
        tabScreen = TabScreens.TabAddContent,
    )

    data object TabFriends : BottomNavigation(
        label = R.string.tab_friends,
        icon = Icons.Rounded.PeopleOutline,
        tabScreen = TabScreens.TabFriends,
    )

    data object TabProfile : BottomNavigation(
        label = R.string.tab_profile,
        icon = Icons.Rounded.PersonOutline,
        tabScreen = TabScreens.TabProfile,
    )
}
