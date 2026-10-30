package com.marta.habittracker.presentation.screens.bottom_nav_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import com.marta.habittracker.presentation.theme.isHabitDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marta.habittracker.presentation.navigation.BottomNavigation
import com.marta.habittracker.presentation.navigation.NavigationBottomWrapper
import com.marta.habittracker.presentation.navigation.TabScreens
import com.marta.habittracker.presentation.utils.SyncStatusBarIcons
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
            destinationRoute.contains(TabScreens.TabEditHabit::class.qualifiedName.toString()) ||
            destinationRoute.contains(TabScreens.TabPreferences::class.qualifiedName.toString()) ||
            destinationRoute.contains(TabScreens.TabGoals::class.qualifiedName.toString()) ||
            destinationRoute.contains(TabScreens.TabRecommendations::class.qualifiedName.toString()) ||
            destinationRoute.contains(TabScreens.TabDeleteAccount::class.qualifiedName.toString()) ||
            destinationRoute.contains(TabScreens.TabChallengeDetail::class.qualifiedName.toString())

    val darkTop = destinationRoute.contains(TabScreens.TabHome::class.qualifiedName.toString()) ||
        destinationRoute.contains(TabScreens.TabProfile::class.qualifiedName.toString()) ||
        destinationRoute.contains(TabScreens.TabAddContent::class.qualifiedName.toString()) ||
        destinationRoute.contains(TabScreens.TabEditHabit::class.qualifiedName.toString())
    SyncStatusBarIcons(lightBackground = !isHabitDarkTheme() && !darkTop)

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
        modifier = Modifier.fillMaxSize(),
        // Top inset: each tab screen draws its own header under the status bar.
        // Bottom inset: handled by NavigationBar defaults (do not pad the whole Scaffold).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
    val itemColors = habitBottomBarItemColors()
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        BottomNavigation.tabBarItems.forEach { tab ->
            if (tab == BottomNavigation.TabNew) {
                NewHabitBarButton(
                    onClick = onAdd,
                    modifier = Modifier.weight(1f),
                )
            } else {
                NavigationBarItem(
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    colors = itemColors,
                    icon = {
                        Icon(
                            imageVector = if (tab == selectedTab) tab.selectedIcon else tab.icon,
                            contentDescription = stringResource(tab.label),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun habitBottomBarItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = if (isHabitDarkTheme()) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    },
    indicatorColor = if (isHabitDarkTheme()) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    },
)

@Composable
private fun NewHabitBarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 4.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BottomNavigation.TabNew.icon,
                    contentDescription = stringResource(BottomNavigation.TabNew.label),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

// @Preview(showBackground = true, showSystemUi = true)
// @Composable
// private fun BottomNavContentPreview() {
    // InstaDevTheme {
        // BottomNavContent(
            // selectedTab = BottomNavigation.TabHome,
            // tabNavController = rememberNavController(),
            // showBottomBar = true,
            // onAdd = {},
            // onSignedOut = {},
        // )
    // }
// }
