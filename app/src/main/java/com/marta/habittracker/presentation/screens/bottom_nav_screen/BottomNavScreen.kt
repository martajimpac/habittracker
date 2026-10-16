package com.marta.habittracker.presentation.screens.bottom_nav_screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marta.habittracker.presentation.navigation.BottomNavigation
import com.marta.habittracker.presentation.navigation.NavigationBottomWrapper
import com.marta.habittracker.presentation.navigation.TabScreens
import com.marta.habittracker.presentation.theme.InstaDevTheme
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras

@Composable
fun BottomNavScreen(
    onSignedOut: () -> Unit,
    initialTabRoute: String? = null,
    initialHabitId: String? = null,
    onHabitIdConsumed: () -> Unit = {},
    initialChallengeId: String? = null,
    onChallengeIdConsumed: () -> Unit = {},
    initialFriendRequestId: String? = null,
    onFriendRequestConsumed: () -> Unit = {},
    bottomNavViewModel: BottomNavViewModel = hiltViewModel(),
) {
    val tabNavController = rememberNavController()
    LaunchedEffect(initialTabRoute, initialHabitId, initialChallengeId, initialFriendRequestId) {
        val habitId = initialHabitId
        if (!habitId.isNullOrBlank()) {
            tabNavController.navigate(TabScreens.TabDetail(habitId)) {
                popUpTo(TabScreens.TabHome) { inclusive = false }
                launchSingleTop = true
            }
            onHabitIdConsumed()
            onChallengeIdConsumed()
            return@LaunchedEffect
        }
        val challengeId = initialChallengeId
        if (!challengeId.isNullOrBlank()) {
            tabNavController.navigate(TabScreens.TabChallengeDetail(challengeId)) {
                popUpTo(TabScreens.TabHome) { inclusive = false }
                launchSingleTop = true
            }
            onChallengeIdConsumed()
            return@LaunchedEffect
        }
        when (initialTabRoute) {
            WidgetLaunchExtras.TAB_HOME -> tabNavController.navigate(TabScreens.TabHome) {
                launchSingleTop = true
            }

            WidgetLaunchExtras.TAB_STATS -> tabNavController.navigate(TabScreens.TabStats) {
                launchSingleTop = true
            }

            WidgetLaunchExtras.TAB_FRIENDS -> {
                tabNavController.navigate(TabScreens.TabFriends) {
                    launchSingleTop = true
                }
                if (!initialFriendRequestId.isNullOrBlank()) {
                    onFriendRequestConsumed()
                }
            }
        }
    }
    val navStackEntry by tabNavController.currentBackStackEntryAsState()
    val destinationRoute = navStackEntry?.destination?.route.orEmpty()

    val selectedTab = BottomNavigation.destinationTabs.firstOrNull { tab ->
        destinationRoute.contains(tab.tabScreen::class.qualifiedName.toString())
    } ?: BottomNavigation.TabHome

    val hideBottomBar =
        destinationRoute.contains(TabScreens.TabAddContent::class.qualifiedName.toString()) ||
            destinationRoute.contains(TabScreens.TabDetail::class.qualifiedName.toString()) ||
            destinationRoute.contains(TabScreens.TabPreferences::class.qualifiedName.toString()) ||
            destinationRoute.contains(TabScreens.TabDeleteAccount::class.qualifiedName.toString()) ||
            destinationRoute.contains(TabScreens.TabChallengeDetail::class.qualifiedName.toString())

    BottomNavContent(
        selectedTab = selectedTab,
        tabNavController = tabNavController,
        showBottomBar = !hideBottomBar,
        onAdd = { tabNavController.navigate(TabScreens.TabAddContent) },
        onSignedOut = onSignedOut,
    )
}

@Composable
fun BottomNavContent(
    selectedTab: BottomNavigation,
    tabNavController: NavHostController,
    showBottomBar: Boolean,
    onAdd: () -> Unit,
    onSignedOut: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .safeDrawingPadding(),
        bottomBar = {
            if (showBottomBar) {
                HabitBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        if (tab != selectedTab) {
                            tabNavController.navigate(tab.tabScreen) {
                                popUpTo(tabNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onAdd = onAdd,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        NavigationBottomWrapper(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            navHostController = tabNavController,
            onSignedOut = onSignedOut,
        )
    }
}

@Composable
fun HabitBottomBar(
    selectedTab: BottomNavigation,
    onTabSelected: (BottomNavigation) -> Unit,
    onAdd: () -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        windowInsets = WindowInsets(0),
    ) {
        BottomNavigation.tabBarItems.forEach { tab ->
            val selected = tab != BottomNavigation.TabNew && tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { if (tab == BottomNavigation.TabNew) onAdd() else onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BottomNavContentPreview() {
    InstaDevTheme {
        BottomNavContent(
            selectedTab = BottomNavigation.TabHome,
            tabNavController = rememberNavController(),
            showBottomBar = true,
            onAdd = {},
            onSignedOut = {},
        )
    }
}
