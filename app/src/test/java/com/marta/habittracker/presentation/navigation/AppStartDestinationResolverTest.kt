package com.marta.habittracker.presentation.navigation

import com.marta.habittracker.presentation.navigation.AppStartDestination
import com.marta.habittracker.presentation.navigation.resolveAppStartDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStartDestinationResolverTest {

    @Test
    fun `when onboarding not completed then start at onboarding`() {
        val destination = resolveAppStartDestination(
            onboardingCompleted = false,
            isLoggedIn = false,
        )
        assertEquals(AppStartDestination.Onboarding, destination)
    }

    @Test
    fun `when onboarding completed and not logged in then start at login`() {
        val destination = resolveAppStartDestination(
            onboardingCompleted = true,
            isLoggedIn = false,
        )
        assertEquals(AppStartDestination.Login, destination)
    }

    @Test
    fun `when onboarding completed and logged in then start at home`() {
        val destination = resolveAppStartDestination(
            onboardingCompleted = true,
            isLoggedIn = true,
        )
        assertEquals(AppStartDestination.Home, destination)
    }

    @Test
    fun `when onboarding not completed ignores login state`() {
        val destination = resolveAppStartDestination(
            onboardingCompleted = false,
            isLoggedIn = true,
        )
        assertEquals(AppStartDestination.Onboarding, destination)
    }
}
