package com.aristidevs.habittracker.view.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aristidevs.habittracker.view.core.navigation.TabScreens.*
import com.aristidevs.habittracker.view.add_content.AddContentScreen
import com.aristidevs.habittracker.view.home.HomeScreen
import com.aristidevs.habittracker.view.profile.ProfileScreen

@Composable
fun NavigationBottomWrapper(modifier: Modifier = Modifier, navHostController: NavHostController) {
    NavHost(modifier = modifier, navController = navHostController, startDestination = TabHome) {
        composable<TabHome> {
            HomeScreen()
        }
        composable<TabAddContent> {
            AddContentScreen()
        }
        composable<TabProfile> {
            ProfileScreen()
        }
    }
}