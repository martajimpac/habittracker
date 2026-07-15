@file:OptIn(ExperimentalMaterial3Api::class)

package com.marta.habittracker.view.screens.bottom_nav_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marta.habittracker.R
import com.marta.habittracker.view.core.components.CustomBadgeBox
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
        topBar = { MyToolbar() },
        bottomBar = {
            MyBottomBar(
                selectedTab = selectedTab,
                navHost = tabNavController
            )
        }
    ) { innerPadding ->
        NavigationBottomWrapper(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            navHostController = tabNavController
        )
    }
}

@Composable
fun MyBottomBar(selectedTab: BottomNavigation, navHost: NavHostController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp
    ) {
        tabBottomItemsList.forEach { tabItem ->
            val isSelected = tabItem == selectedTab
            NavigationBarItem(
                selected = isSelected,
                icon = {
                    Icon(
                        modifier = Modifier.size(26.dp),
                        imageVector = if (isSelected)
                            tabItem.iconSelected
                        else
                            tabItem.iconUnselected,
                        contentDescription = null
                    )
                },
                label = { 
                    Text(
                        text = stringResource(tabItem.label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ) 
                },
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
                }
            )
        }
    }
}

@Composable
fun MyToolbar() {
    TopAppBar(
        modifier = Modifier.padding(end = 16.dp), colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ), title = {
            Icon(
                painter = painterResource(R.drawable.ic_instagram_title),
                contentDescription = "InstaDev Title logo",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .height(44.dp)
                    .padding(top = 4.dp)
            )
        }, actions = {
            CustomBadgeBox(painter = painterResource(R.drawable.ic_like), notificationNumber = 2)
            Spacer(Modifier.width(16.dp))
            CustomBadgeBox(painter = painterResource(R.drawable.ic_send), notificationNumber = 1)
        })
}

