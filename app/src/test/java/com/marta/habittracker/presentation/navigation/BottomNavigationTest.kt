package com.marta.habittracker.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavigationTest {

    @Test
    fun tabBarItems_haveNewInCenter() {
        val items = BottomNavigation.tabBarItems
        assertEquals(5, items.size)
        assertEquals(BottomNavigation.TabHome, items[0])
        assertEquals(BottomNavigation.TabStats, items[1])
        assertEquals(BottomNavigation.TabNew, items[2])
        assertEquals(BottomNavigation.TabFriends, items[3])
        assertEquals(BottomNavigation.TabProfile, items[4])
    }

    @Test
    fun destinationTabs_excludeNew() {
        val destinations = BottomNavigation.destinationTabs
        assertEquals(4, destinations.size)
        assertFalse(destinations.contains(BottomNavigation.TabNew))
        assertTrue(destinations.contains(BottomNavigation.TabHome))
        assertEquals(TabScreens.TabAddContent, BottomNavigation.TabNew.tabScreen)
    }

    @Test
    fun destinationTabs_swapOutlineForFilledWhenSelected() {
        BottomNavigation.destinationTabs.forEach { tab ->
            assertNotEquals(tab.icon, tab.selectedIcon)
        }
        assertEquals(BottomNavigation.TabNew.icon, BottomNavigation.TabNew.selectedIcon)
    }
}
