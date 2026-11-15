package com.marta.habittracker.presentation.navigation

import androidx.navigation.NavHostController

/**
 * Switches bottom-nav tab keeping each tab's back stack.
 *
 * Every tab switch (bottom bar, widgets, notifications) must go through here. Navigation maps a
 * non-inclusive `popUpTo(start) { saveState = true }` to the start destination the first time it
 * runs. If a tab was pushed without these options (e.g. from a widget), the next tap on Home saves
 * that tab's stack under Home and `restoreState` brings the tab back instead of Home.
 */
fun NavHostController.navigateToTab(tab: TabScreens) {
    navigate(tab) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
