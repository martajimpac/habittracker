package com.marta.habittracker.view.core.navigation

sealed interface AppStartDestination {
    data object Onboarding : AppStartDestination
    data object Login : AppStartDestination
    data object Home : AppStartDestination
}

fun resolveAppStartDestination(
    onboardingCompleted: Boolean,
    isLoggedIn: Boolean,
): AppStartDestination = when {
    !onboardingCompleted -> AppStartDestination.Onboarding
    !isLoggedIn -> AppStartDestination.Login
    else -> AppStartDestination.Home
}
