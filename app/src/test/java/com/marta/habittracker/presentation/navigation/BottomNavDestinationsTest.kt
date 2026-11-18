package com.marta.habittracker.presentation.navigation

import android.app.Application
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Bottom bar state must be derived from the destination's serializable route, never from
 * `KClass.qualifiedName`: R8 renames the classes in release builds, so name-based matching
 * always fell back to Home.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class BottomNavDestinationsTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            graph = createGraph(startDestination = TabScreens.TabHome) {
                composable<TabScreens.TabHome> {}
                composable<TabScreens.TabStats> {}
                composable<TabScreens.TabFriends> {}
                composable<TabScreens.TabProfile> {}
                composable<TabScreens.TabAddContent> {}
                composable<TabScreens.TabPreferences> {}
                composable<TabScreens.TabGoals> {}
                composable<TabScreens.TabRecommendations> {}
                composable<TabScreens.TabDeleteAccount> {}
                composable<TabScreens.TabDetail> {}
                composable<TabScreens.TabEditHabit> {}
                composable<TabScreens.TabChallengeDetail> {}
            }
        }
    }

    @Test
    fun startDestination_selectsHome() {
        assertEquals(BottomNavigation.TabHome, navController.currentDestination.selectedBottomTab())
    }

    @Test
    fun switchingTab_selectsThatTab() {
        BottomNavigation.destinationTabs.forEach { tab ->
            navController.navigateToTab(tab.tabScreen)

            assertEquals(tab, navController.currentDestination.selectedBottomTab())
        }
    }

    @Test
    fun switchingBackToHome_selectsHome() {
        navController.navigateToTab(TabScreens.TabStats)
        navController.navigateToTab(TabScreens.TabHome)

        assertEquals(BottomNavigation.TabHome, navController.currentDestination.selectedBottomTab())
    }

    @Test
    fun noDestination_fallsBackToHome() {
        assertEquals(BottomNavigation.TabHome, null.selectedBottomTab())
    }

    @Test
    fun mainTabs_showBottomBar() {
        BottomNavigation.destinationTabs.forEach { tab ->
            navController.navigateToTab(tab.tabScreen)

            assertFalse(navController.currentDestination.hidesBottomBar())
        }
    }

    @Test
    fun fullScreenDestinations_hideBottomBar() {
        listOf(
            TabScreens.TabAddContent,
            TabScreens.TabPreferences,
            TabScreens.TabGoals,
            TabScreens.TabRecommendations,
            TabScreens.TabDeleteAccount,
            TabScreens.TabDetail("habit-1"),
            TabScreens.TabEditHabit("habit-1"),
            TabScreens.TabChallengeDetail("challenge-1"),
        ).forEach { route ->
            navController.navigate(route)

            assertTrue(
                "$route should hide the bottom bar",
                navController.currentDestination.hidesBottomBar(),
            )
        }
    }

    @Test
    fun darkTop_onlyForHeaderScreens() {
        navController.navigateToTab(TabScreens.TabHome)
        assertTrue(navController.currentDestination.hasDarkTop())

        navController.navigateToTab(TabScreens.TabProfile)
        assertTrue(navController.currentDestination.hasDarkTop())

        navController.navigateToTab(TabScreens.TabStats)
        assertFalse(navController.currentDestination.hasDarkTop())

        navController.navigate(TabScreens.TabEditHabit("habit-1"))
        assertTrue(navController.currentDestination.hasDarkTop())
    }
}
