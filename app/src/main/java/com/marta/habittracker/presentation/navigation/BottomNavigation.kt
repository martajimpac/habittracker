package com.marta.habittracker.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.rounded.ShowChart as ShowChartFilled
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home as HomeFilled
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.marta.habittracker.R

sealed class BottomNavigation(
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val tabScreen: TabScreens,
) {
    companion object {
        val destinationTabs = listOf(TabHome, TabStats, TabFriends, TabProfile)
        val tabBarItems = listOf(TabHome, TabStats, TabNew, TabFriends, TabProfile)
    }

    data object TabHome : BottomNavigation(
        label = R.string.tab_home,
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Rounded.HomeFilled,
        tabScreen = TabScreens.TabHome,
    )

    data object TabStats : BottomNavigation(
        label = R.string.tab_stats,
        icon = Icons.AutoMirrored.Outlined.ShowChart,
        selectedIcon = Icons.AutoMirrored.Rounded.ShowChartFilled,
        tabScreen = TabScreens.TabStats,
    )

    data object TabNew : BottomNavigation(
        label = R.string.tab_new,
        icon = Icons.Rounded.Add,
        selectedIcon = Icons.Rounded.Add,
        tabScreen = TabScreens.TabAddContent,
    )

    data object TabFriends : BottomNavigation(
        label = R.string.tab_friends,
        icon = Icons.Rounded.PeopleOutline,
        selectedIcon = Icons.Rounded.People,
        tabScreen = TabScreens.TabFriends,
    )

    data object TabProfile : BottomNavigation(
        label = R.string.tab_profile,
        icon = Icons.Rounded.PersonOutline,
        selectedIcon = Icons.Rounded.Person,
        tabScreen = TabScreens.TabProfile,
    )
}
