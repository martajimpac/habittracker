@file:OptIn(ExperimentalMaterial3Api::class)

package com.aristidevs.habittracker.view.bottom_nav_screen

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aristidevs.habittracker.R
import com.aristidevs.habittracker.view.core.components.InstaBadgeBox
import com.aristidevs.habittracker.view.core.navigation.BottomNavigation
import com.aristidevs.habittracker.view.core.navigation.BottomNavigation.Companion.tabBottomItemsList
import com.aristidevs.habittracker.view.core.navigation.NavigationBottomWrapper

@Composable
fun BottomNavScreen(bottomNavViewModel: BottomNavViewModel = hiltViewModel()) {

    val tabNavController = rememberNavController()
    val navStackEntry by tabNavController.currentBackStackEntryAsState()

    val destination = navStackEntry?.destination
    val currentRoute = destination?.route?.substringBefore("?")

    val selectedTab = tabBottomItemsList.firstOrNull { tab ->
        tab.tabScreen::class.qualifiedName == currentRoute?.substringBefore("?")
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
                .padding(innerPadding), navHostController = tabNavController
        )
    }

}

@Composable
fun MyBottomBar(selectedTab: BottomNavigation, navHost: NavHostController) {
    NavigationBar {
        tabBottomItemsList.forEach { tabItem ->
            val isSelected = tabItem == selectedTab
            NavigationBarItem(
                selected = isSelected,
                icon = {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(tabItem.icon),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(tabItem.label)) },
                onClick = {
                    if (!isSelected) {
                        navHost.navigate(tabItem.tabScreen) {
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
            InstaBadgeBox(painter = painterResource(R.drawable.ic_like), notificationNumber = 2)
            Spacer(Modifier.width(16.dp))
            InstaBadgeBox(painter = painterResource(R.drawable.ic_send), notificationNumber = 1)
        })
}

