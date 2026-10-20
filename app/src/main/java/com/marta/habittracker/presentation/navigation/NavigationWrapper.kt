package com.marta.habittracker.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.marta.habittracker.presentation.auth.AuthDeeplinkEvent
import com.marta.habittracker.presentation.auth.AuthDeeplinkViewModel
import com.marta.habittracker.presentation.components.HabitLoadingIndicator
import com.marta.habittracker.presentation.screens.auth.forgot_password.ForgotPasswordScreen
import com.marta.habittracker.presentation.screens.auth.login.LoginScreen
import com.marta.habittracker.presentation.screens.auth.register.RegisterScreen
import com.marta.habittracker.presentation.screens.auth.reset_password.ResetPasswordScreen
import com.marta.habittracker.presentation.screens.bottom_nav_screen.BottomNavScreen
import com.marta.habittracker.presentation.screens.onboarding.OnboardingScreen
import com.marta.habittracker.presentation.utils.ObserveAsEvents

@Composable
fun NavigationWrapper(
    initialTabRoute: String? = null,
    initialHabitId: String? = null,
    onHabitIdConsumed: () -> Unit = {},
    initialChallengeId: String? = null,
    onChallengeIdConsumed: () -> Unit = {},
    initialFriendRequestId: String? = null,
    onFriendRequestConsumed: () -> Unit = {},
    appStartViewModel: AppStartViewModel = hiltViewModel(),
    authDeeplinkViewModel: AuthDeeplinkViewModel = hiltViewModel(),
    loginPromptCoordinator: LoginPromptCoordinator = hiltViewModel<LoginPromptSender>().coordinator,
) {
    val startDestination by appStartViewModel.startDestination.collectAsStateWithLifecycle()

    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            HabitLoadingIndicator()
        }
        return
    }

    val navController = rememberNavController()

    fun openLogin(message: LoginMessage? = null) {
        if (message != null) loginPromptCoordinator.show(message)
        navController.navigate(Login) {
            popUpTo(0) { inclusive = true }
        }
    }
    val destination = checkNotNull(startDestination)
    val route = when (destination) {
        AppStartDestination.Onboarding -> Onboarding
        AppStartDestination.Login -> Login
        AppStartDestination.Home -> Home
    }

    LaunchedEffect(initialChallengeId) {
        if (initialChallengeId.isNullOrBlank()) return@LaunchedEffect
        snapshotFlow { navController.currentBackStackEntry }
            .collect { entry ->
                if (entry == null) return@collect
                if (!entry.destination.hasRoute<Home>()) {
                    onChallengeIdConsumed()
                }
            }
    }

    LaunchedEffect(initialFriendRequestId) {
        if (initialFriendRequestId.isNullOrBlank()) return@LaunchedEffect
        snapshotFlow { navController.currentBackStackEntry }
            .collect { entry ->
                if (entry == null) return@collect
                if (!entry.destination.hasRoute<Home>()) {
                    onFriendRequestConsumed()
                }
            }
    }

    ObserveAsEvents(flow = authDeeplinkViewModel.events) { event ->
        when (event) {
            AuthDeeplinkEvent.NavigateToResetPassword -> {
                navController.navigate(ResetPassword) {
                    popUpTo(0) { inclusive = true }
                }
            }

            AuthDeeplinkEvent.ResetLinkFailed -> {
                openLogin(LoginMessage.ResetLinkInvalid)
            }

            AuthDeeplinkEvent.NavigateHome -> {
                navController.navigate(Home) {
                    popUpTo(0) { inclusive = true }
                }
            }

            AuthDeeplinkEvent.EmailConfirmed -> {
                openLogin(LoginMessage.EmailConfirmed)
            }

            AuthDeeplinkEvent.EmailConfirmFailed -> {
                openLogin(LoginMessage.ResetLinkInvalid)
            }
        }
    }

    NavHost(navController = navController, startDestination = route) {
        composable<Onboarding> {
            OnboardingScreen(
                navigateToLogin = {
                    openLogin()
                }
            )
        }

        composable<Login> {
            LoginScreen(
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
                    openLogin(LoginMessage.PasswordResetRequestSent)
                },
            )
        }

        composable<ResetPassword> {
            ResetPasswordScreen(
                navigateToLogin = {
                    openLogin(LoginMessage.PasswordUpdated)
                },
            )
        }

        composable<Register> {
            RegisterScreen(
                navigateBack = { navController.popBackStack() },
                navigateToLogin = {
                    openLogin(LoginMessage.AccountCreated)
                },
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
                initialHabitId = initialHabitId,
                onHabitIdConsumed = onHabitIdConsumed,
                initialChallengeId = initialChallengeId,
                onChallengeIdConsumed = onChallengeIdConsumed,
                initialFriendRequestId = initialFriendRequestId,
                onFriendRequestConsumed = onFriendRequestConsumed,
                onSignedOut = {
                    openLogin()
                },
            )
        }
    }
}
