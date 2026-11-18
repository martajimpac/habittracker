package com.marta.habittracker.presentation.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlin.reflect.KClass

/*
 * Bottom bar state derived from the current tab destination.
 *
 * Always match with `hasRoute`, which compares the route's serializer. Never compare
 * `destination.route` with `KClass.qualifiedName`: R8 renames the classes in release builds,
 * so the names stop matching and the bar always highlighted Home.
 */

private val hideBottomBarRoutes: List<KClass<out TabScreens>> = listOf(
    TabScreens.TabAddContent::class,
    TabScreens.TabDetail::class,
    TabScreens.TabEditHabit::class,
    TabScreens.TabPreferences::class,
    TabScreens.TabGoals::class,
    TabScreens.TabRecommendations::class,
    TabScreens.TabDeleteAccount::class,
    TabScreens.TabChallengeDetail::class,
)

private val darkTopRoutes: List<KClass<out TabScreens>> = listOf(
    TabScreens.TabHome::class,
    TabScreens.TabProfile::class,
    TabScreens.TabAddContent::class,
    TabScreens.TabEditHabit::class,
)

/** Tab to highlight in the bottom bar; Home when the destination is not a main tab. */
fun NavDestination?.selectedBottomTab(): BottomNavigation =
    BottomNavigation.destinationTabs.firstOrNull { tab -> isTabRoute(tab.tabScreen::class) }
        ?: BottomNavigation.TabHome

/** Full-screen destinations (forms, details, settings) draw without the bottom bar. */
fun NavDestination?.hidesBottomBar(): Boolean = hideBottomBarRoutes.any { isTabRoute(it) }

/** Destinations whose header sits under the status bar and needs light status bar icons. */
fun NavDestination?.hasDarkTop(): Boolean = darkTopRoutes.any { isTabRoute(it) }

@Suppress("UNCHECKED_CAST")
private fun NavDestination?.isTabRoute(route: KClass<out TabScreens>): Boolean =
    this != null && hasRoute(route as KClass<TabScreens>)
