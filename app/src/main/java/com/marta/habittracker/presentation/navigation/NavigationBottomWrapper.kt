package com.marta.habittracker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.marta.habittracker.presentation.navigation.TabScreens.*
import com.marta.habittracker.presentation.screens.add_content.AddContentScreen
import com.marta.habittracker.presentation.screens.detail.HabitDetailScreen
import com.marta.habittracker.presentation.screens.home.HomeScreen
import com.marta.habittracker.presentation.screens.profile.ProfileScreen
import com.marta.habittracker.presentation.screens.stats.StatsScreen

@Composable
fun NavigationBottomWrapper(modifier: Modifier = Modifier, navHostController: NavHostController) {
    NavHost(modifier = modifier, navController = navHostController, startDestination = TabHome) {

        composable<TabHome> {
            HomeScreen(
                onAdd = { navHostController.navigate(TabAddContent) },
            )
        }

        composable<TabStats> { StatsScreen() }
        composable<TabAddContent> {
            AddContentScreen(
                onBack = { navHostController.popBackStack() },
            )
        }
        composable<TabProfile> { ProfileScreen() }

        composable<TabDetail> {
            HabitDetailScreen(
                onBack = { navHostController.popBackStack() }
            )
        }
    }
}
