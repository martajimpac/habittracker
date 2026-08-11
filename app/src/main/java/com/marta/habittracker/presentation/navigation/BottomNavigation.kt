package com.marta.habittracker.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.marta.habittracker.R

sealed class BottomNavigation(
    @StringRes val label: Int,
    val icon: ImageVector,
    val tabScreen: TabScreens,
) {
    companion object {
        val tabBottomItemsList = listOf(TabHome, TabStats, TabFriends, TabProfile)
    }

    data object TabHome : BottomNavigation(
        label = R.string.tab_home,
        icon = Icons.Outlined.Home,
        tabScreen = TabScreens.TabHome,
    )

    data object TabStats : BottomNavigation(
        label = R.string.tab_stats,
        icon = Icons.AutoMirrored.Outlined.ShowChart,
        tabScreen = TabScreens.TabStats,
    )

    data object TabFriends : BottomNavigation(
        label = R.string.tab_friends,
        icon = Icons.Outlined.PeopleOutline,
        tabScreen = TabScreens.TabFriends,
    )

    data object TabProfile : BottomNavigation(
        label = R.string.tab_profile,
        icon = Icons.Outlined.PersonOutline,
        tabScreen = TabScreens.TabProfile,
    )
}
