package com.marta.habittracker.view.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.marta.habittracker.R

sealed class BottomNavigation(
    @StringRes val label: Int,
    val iconUnselected: ImageVector,
    val iconSelected: ImageVector,
    val tabScreen: TabScreens
) {
    companion object {
        val tabBottomItemsList = listOf(TabHome, TabAddContent, TabProfile)
    }

    data object TabHome : BottomNavigation(
        label = R.string.tab_home,
        iconUnselected = Icons.Outlined.Home,
        iconSelected = Icons.Filled.Home,
        tabScreen = TabScreens.TabHome
    )

    data object TabAddContent : BottomNavigation(
        label = R.string.tab_add_content,
        iconUnselected = Icons.Outlined.AddCircleOutline,
        iconSelected = Icons.Filled.AddCircle,
        tabScreen = TabScreens.TabAddContent
    )

    data object TabProfile : BottomNavigation(
        label = R.string.tab_profile,
        iconUnselected = Icons.Outlined.Person,
        iconSelected = Icons.Filled.Person,
        tabScreen = TabScreens.TabProfile
    )
}
