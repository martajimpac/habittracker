package com.aristidevs.habittracker.view.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aristidevs.habittracker.R

sealed class BottomNavigation(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    val tabScreen: TabScreens
) {
    companion object {
        val tabBottomItemsList = listOf(TabHome, TabAddContent, TabProfile)
    }

    data object TabHome : BottomNavigation(
        label = R.string.tab_home,
        icon = R.drawable.ic_home,
        tabScreen = TabScreens.TabHome
    )

    data object TabAddContent : BottomNavigation(
        label = R.string.tab_add_content,
        icon = R.drawable.ic_add,
        tabScreen = TabScreens.TabAddContent
    )

    data object TabProfile : BottomNavigation(
        label = R.string.tab_profile,
        icon = R.drawable.ic_profile,
        tabScreen = TabScreens.TabProfile
    )
}
