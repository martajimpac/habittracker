
package com.aristidevs.habittracker.view.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aristidevs.habittracker.view.core.navigation.TabScreens.*
import com.aristidevs.habittracker.view.screens.add_content.AddContentScreen
import com.aristidevs.habittracker.view.screens.detail.HabitDetailScreen
import com.aristidevs.habittracker.view.screens.home.HomeScreen
import com.aristidevs.habittracker.view.screens.profile.ProfileScreen

@Composable
fun NavigationBottomWrapper(modifier: Modifier = Modifier, navHostController: NavHostController) {
    NavHost(modifier = modifier, navController = navHostController, startDestination = TabHome) {

        composable<TabHome> {
            HomeScreen(
                onHabitClick = { id ->
                    // Navegamos usando la clase HabitDetail
                    navHostController.navigate(TabDetail(habitId = id))
                }
            )
        }

        composable<TabAddContent> { AddContentScreen() }
        composable<TabProfile> { ProfileScreen() }

        composable<TabDetail> {
            HabitDetailScreen(
                onBack = { navHostController.popBackStack() }
            )
        }


    }
}