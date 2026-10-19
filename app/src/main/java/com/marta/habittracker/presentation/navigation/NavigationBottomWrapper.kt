package com.marta.habittracker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.marta.habittracker.presentation.navigation.TabScreens.TabAddContent
import com.marta.habittracker.presentation.navigation.TabScreens.TabDeleteAccount
import com.marta.habittracker.presentation.navigation.TabScreens.TabChallengeDetail
import com.marta.habittracker.presentation.navigation.TabScreens.TabDetail
import com.marta.habittracker.presentation.navigation.TabScreens.TabEditHabit
import com.marta.habittracker.presentation.navigation.TabScreens.TabFriends
import com.marta.habittracker.presentation.navigation.TabScreens.TabHome
import com.marta.habittracker.presentation.navigation.TabScreens.TabGoals
import com.marta.habittracker.presentation.navigation.TabScreens.TabPreferences
import com.marta.habittracker.presentation.navigation.TabScreens.TabProfile
import com.marta.habittracker.presentation.navigation.TabScreens.TabStats
import com.marta.habittracker.presentation.screens.add_content.AddContentScreen
import com.marta.habittracker.presentation.screens.deleteaccount.DeleteAccountScreen
import com.marta.habittracker.presentation.screens.challenge.ChallengeDetailScreen
import com.marta.habittracker.presentation.screens.detail.HabitDetailScreen
import com.marta.habittracker.presentation.screens.friends.FriendsScreen
import com.marta.habittracker.presentation.screens.goals.GoalsScreen
import com.marta.habittracker.presentation.screens.home.HomeScreen
import com.marta.habittracker.presentation.screens.preferences.PreferencesScreen
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
            HomeScreen()
        }

        composable<TabStats> {
            StatsScreen(
                onHabitClick = { habitId ->
                    navHostController.navigate(TabDetail(habitId))
                },
            )
        }

        composable<TabFriends> {
            FriendsScreen(
                onOpenChallenge = { id ->
                    navHostController.navigate(TabChallengeDetail(id))
                },
            )
        }

        composable<TabAddContent> {
            AddContentScreen(
                onBack = { navHostController.popBackStack() },
            )
        }

        composable<TabProfile> {
            ProfileScreen(
                onSignedOut = onSignedOut,
                onPreferences = { navHostController.navigate(TabPreferences) },
                onGoals = { navHostController.navigate(TabGoals) },
            )
        }

        composable<TabGoals> {
            GoalsScreen(
                onBack = { navHostController.popBackStack() },
            )
        }

        composable<TabPreferences> {
            PreferencesScreen(
                onBack = { navHostController.popBackStack() },
                onDeleteAccount = { navHostController.navigate(TabDeleteAccount) },
            )
        }

        composable<TabDeleteAccount> {
            DeleteAccountScreen(
                onBack = { navHostController.popBackStack() },
                onSignedOut = onSignedOut,
            )
        }

        composable<TabDetail> {
            HabitDetailScreen(
                onBack = { navHostController.popBackStack() },
                onEdit = { habitId ->
                    navHostController.navigate(TabEditHabit(habitId))
                },
            )
        }

        composable<TabEditHabit> {
            AddContentScreen(
                onBack = { navHostController.popBackStack() },
            )
        }

        composable<TabChallengeDetail> {
            ChallengeDetailScreen(
                onBack = { navHostController.popBackStack() },
            )
        }
    }
}
