package com.marta.habittracker.view.screens.bottom_nav_screen

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marta.habittracker.ui.theme.HabitOnSurfaceVariant
import com.marta.habittracker.ui.theme.HabitPrimary
import com.marta.habittracker.ui.theme.HabitSurface
import com.marta.habittracker.ui.theme.HabitTermsBg
import com.marta.habittracker.view.core.navigation.BottomNavigation
import com.marta.habittracker.view.core.navigation.BottomNavigation.Companion.tabBottomItemsList
import com.marta.habittracker.view.core.navigation.NavigationBottomWrapper

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
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabBottomItemsList.forEach { tabItem ->
            val isSelected = tabItem == selectedTab
            Column(
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) HabitTermsBg else Color.Transparent)
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSelected) tabItem.iconSelected else tabItem.iconUnselected,
                        contentDescription = null,
                        tint = if (isSelected) HabitPrimary else HabitOnSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(tabItem.label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (isSelected) HabitPrimary else HabitOnSurfaceVariant,
                )
            }
        }
    }
}
