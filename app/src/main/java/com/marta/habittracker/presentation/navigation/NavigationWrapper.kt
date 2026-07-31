package com.marta.habittracker.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.marta.habittracker.presentation.screens.auth.login.LoginScreen
import com.marta.habittracker.presentation.screens.auth.register.RegisterScreen
import com.marta.habittracker.presentation.screens.bottom_nav_screen.BottomNavScreen
import com.marta.habittracker.presentation.screens.onboarding.OnboardingScreen

@Composable
fun NavigationWrapper(
    appStartViewModel: AppStartViewModel = hiltViewModel(),
) {
    val startDestination by appStartViewModel.startDestination.collectAsStateWithLifecycle()

    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val destination = checkNotNull(startDestination)
    val route = when (destination) {
        AppStartDestination.Onboarding -> Onboarding
        AppStartDestination.Login -> Login
        AppStartDestination.Home -> Home
    }

    NavHost(navController = navController, startDestination = route) {
        composable<Onboarding> {
            OnboardingScreen(
                navigateToLogin = {
                    navController.navigate(Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<Login> {
            LoginScreen(
                navigateToRegister = { navController.navigate(Register) },
                navigateToHome = {
                    navController.navigate(Home) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<Register> {
            RegisterScreen(
                navigateBack = { navController.popBackStack() },
                navigateToHome = {
                    navController.navigate(Home) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<Home> {
            BottomNavScreen(
                onSignedOut = {
                    navController.navigate(Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
