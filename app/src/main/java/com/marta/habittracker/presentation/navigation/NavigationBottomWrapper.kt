package com.marta.habittracker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.marta.habittracker.presentation.navigation.TabScreens.TabAddContent
import com.marta.habittracker.presentation.navigation.TabScreens.TabDetail
import com.marta.habittracker.presentation.navigation.TabScreens.TabFriends
import com.marta.habittracker.presentation.navigation.TabScreens.TabHome
import com.marta.habittracker.presentation.navigation.TabScreens.TabProfile
import com.marta.habittracker.presentation.navigation.TabScreens.TabStats
import com.marta.habittracker.presentation.screens.add_content.AddContentScreen
import com.marta.habittracker.presentation.screens.detail.HabitDetailScreen
import com.marta.habittracker.presentation.screens.friends.FriendsScreen
import com.marta.habittracker.presentation.screens.home.HomeScreen
import com.marta.habittracker.presentation.screens.profile.ProfileScreen
import com.marta.habittracker.presentation.screens.stats.StatsScreen

@Composable
fun NavigationBottomWrapper(
    modifier: Modifier = Modifier,
    navHostController: NavHostController,
    onSignedOut: () -> Unit,
) {
    NavHost(modifier = modifier, navController = navHostController, startDestination = TabHome) {

        composable<TabHome> {
            HomeScreen(
                onAdd = { navHostController.navigate(TabAddContent) },
            )
        }

        composable<TabStats> { StatsScreen() }

        composable<TabFriends> { FriendsScreen() }

        composable<TabAddContent> {
            AddContentScreen(
                onBack = { navHostController.popBackStack() },
            )
        }

        composable<TabProfile> {
            ProfileScreen(onSignedOut = onSignedOut)
        }

        composable<TabDetail> {
            HabitDetailScreen(
                onBack = { navHostController.popBackStack() },
            )
        }
    }
}
