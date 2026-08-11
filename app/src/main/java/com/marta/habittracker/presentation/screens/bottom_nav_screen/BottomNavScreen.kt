package com.marta.habittracker.presentation.screens.bottom_nav_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marta.habittracker.R
import com.marta.habittracker.presentation.navigation.BottomNavigation
import com.marta.habittracker.presentation.navigation.BottomNavigation.Companion.tabBottomItemsList
import com.marta.habittracker.presentation.navigation.NavigationBottomWrapper
import com.marta.habittracker.presentation.navigation.TabScreens
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.HabitPrimaryLight
import com.marta.habittracker.presentation.theme.HabitSurface
import com.marta.habittracker.presentation.theme.HabitTermsBg
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras

@Composable
fun BottomNavScreen(
    onSignedOut: () -> Unit,
    initialTabRoute: String? = null,
    bottomNavViewModel: BottomNavViewModel = hiltViewModel(),
) {
    val tabNavController = rememberNavController()
    LaunchedEffect(initialTabRoute) {
        when (initialTabRoute) {
            WidgetLaunchExtras.TAB_HOME -> tabNavController.navigate(TabScreens.TabHome) {
                launchSingleTop = true
            }

            WidgetLaunchExtras.TAB_STATS -> tabNavController.navigate(TabScreens.TabStats) {
                launchSingleTop = true
            }

            WidgetLaunchExtras.TAB_FRIENDS -> tabNavController.navigate(TabScreens.TabFriends) {
                launchSingleTop = true
            }
        }
    }
    val navStackEntry by tabNavController.currentBackStackEntryAsState()
    val destinationRoute = navStackEntry?.destination?.route.orEmpty()

    val selectedTab = tabBottomItemsList.firstOrNull { tab ->
        destinationRoute.contains(tab.tabScreen::class.qualifiedName.toString())
    } ?: BottomNavigation.TabHome

    val hideBottomBar =
        destinationRoute.contains(TabScreens.TabAddContent::class.qualifiedName.toString()) ||
            destinationRoute.contains(TabScreens.TabDetail::class.qualifiedName.toString())

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
        containerColor = HabitSurface,
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
    val leftTabs = listOf(BottomNavigation.TabHome, BottomNavigation.TabStats)
    val rightTabs = listOf(BottomNavigation.TabFriends, BottomNavigation.TabProfile)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(width = 1.dp, color = HabitPrimary.copy(alpha = 0.1f))
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        leftTabs.forEach { tab ->
            BottomTabItem(
                label = stringResource(tab.label),
                icon = tab.icon,
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = HabitPrimary.copy(alpha = 0.45f),
                        spotColor = HabitPrimary.copy(alpha = 0.45f),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(HabitPrimary, HabitPrimaryLight)))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAdd,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.tab_new),
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = stringResource(R.string.tab_new),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = HabitOnSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            )
        }

        rightTabs.forEach { tab ->
            BottomTabItem(
                label = stringResource(tab.label),
                icon = tab.icon,
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BottomTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) HabitPrimary else HabitOnSurfaceVariant
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) HabitTermsBg else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(21.dp),
                tint = tint,
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}
