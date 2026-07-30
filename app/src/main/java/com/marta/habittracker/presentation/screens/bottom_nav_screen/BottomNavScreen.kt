package com.marta.habittracker.presentation.screens.bottom_nav_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marta.habittracker.presentation.navigation.BottomNavigation
import com.marta.habittracker.presentation.navigation.BottomNavigation.Companion.tabBottomItemsList
import com.marta.habittracker.presentation.navigation.NavigationBottomWrapper
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.HabitSurface
import com.marta.habittracker.presentation.theme.HabitTermsBg

@Composable
fun BottomNavScreen(bottomNavViewModel: BottomNavViewModel = hiltViewModel()) {
    val tabNavController = rememberNavController()
    val navStackEntry by tabNavController.currentBackStackEntryAsState()
    val destination = navStackEntry?.destination

    val selectedTab = tabBottomItemsList.firstOrNull { tab ->
        destination?.route?.contains(tab.tabScreen::class.qualifiedName.toString()) == true
    } ?: BottomNavigation.TabHome

    Scaffold(
        bottomBar = {
            HabitBottomBar(
                selectedTab = selectedTab,
                navHost = tabNavController,
            )
        },
        containerColor = HabitSurface,
    ) { innerPadding ->
        NavigationBottomWrapper(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            navHostController = tabNavController,
        )
    }
}

@Composable
fun HabitBottomBar(selectedTab: BottomNavigation, navHost: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(width = 1.dp, color = HabitPrimary.copy(alpha = 0.1f))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabBottomItemsList.forEach { tabItem ->
            val isSelected = tabItem == selectedTab
            val label = stringResource(tabItem.label)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (!isSelected) {
                                navHost.navigate(tabItem.tabScreen) {
                                    popUpTo(navHost.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) HabitTermsBg else Color.Transparent)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tabItem.icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) HabitPrimary else HabitOnSurfaceVariant,
                    )
                }
            }
        }
    }
}
