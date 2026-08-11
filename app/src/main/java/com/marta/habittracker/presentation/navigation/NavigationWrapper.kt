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
import androidx.navigation.toRoute
import com.marta.habittracker.presentation.auth.AuthDeeplinkEvent
import com.marta.habittracker.presentation.auth.AuthDeeplinkViewModel
import com.marta.habittracker.presentation.screens.auth.forgot_password.ForgotPasswordScreen
import com.marta.habittracker.presentation.screens.auth.login.LoginScreen
import com.marta.habittracker.presentation.screens.auth.register.RegisterScreen
import com.marta.habittracker.presentation.screens.auth.reset_password.ResetPasswordScreen
import com.marta.habittracker.presentation.screens.bottom_nav_screen.BottomNavScreen
import com.marta.habittracker.presentation.screens.onboarding.OnboardingScreen
import com.marta.habittracker.presentation.utils.CollectAsEffect

@Composable
fun NavigationWrapper(
    initialTabRoute: String? = null,
    appStartViewModel: AppStartViewModel = hiltViewModel(),
    authDeeplinkViewModel: AuthDeeplinkViewModel = hiltViewModel(),
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
        AppStartDestination.Login -> Login()
        AppStartDestination.Home -> Home
    }

    CollectAsEffect(authDeeplinkViewModel.events) { event ->
        when (event) {
            AuthDeeplinkEvent.NavigateToResetPassword -> {
                navController.navigate(ResetPassword) {
                    popUpTo(0) { inclusive = true }
                }
            }

            AuthDeeplinkEvent.ResetLinkFailed -> {
                navController.navigate(Login(LoginMessage.ResetLinkInvalid)) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = route) {
        composable<Onboarding> {
            OnboardingScreen(
                navigateToLogin = {
                    navController.navigate(Login()) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<Login> { backStackEntry ->
            LoginScreen(
                message = backStackEntry.toRoute<Login>().message,
                navigateToRegister = { navController.navigate(Register) },
                navigateToForgotPassword = { email ->
                    navController.navigate(ForgotPassword(email))
                },
                navigateToHome = {
                    navController.navigate(Home) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<ForgotPassword> { backStackEntry ->
            ForgotPasswordScreen(
                initialEmail = backStackEntry.toRoute<ForgotPassword>().email,
                navigateBack = { navController.popBackStack() },
                navigateToLogin = {
                    navController.navigate(Login(LoginMessage.PasswordResetRequestSent)) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable<ResetPassword> {
            ResetPasswordScreen(
                navigateToLogin = {
                    navController.navigate(Login(LoginMessage.PasswordUpdated)) {
                        popUpTo(0) { inclusive = true }
                    }
                },
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
                initialTabRoute = initialTabRoute,
                onSignedOut = {
                    navController.navigate(Login()) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
