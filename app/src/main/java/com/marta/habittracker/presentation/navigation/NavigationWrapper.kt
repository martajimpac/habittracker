package com.marta.habittracker.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

private const val TAG = "NavigationWrapper"

@Composable
fun NavigationWrapper(
    appStartViewModel: AppStartViewModel = hiltViewModel(),
    authDeeplinkViewModel: AuthDeeplinkViewModel = hiltViewModel(),
    launchTargetViewModel: LaunchTargetViewModel = hiltViewModel(),
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

    fun openLogin(
        message: LoginMessage? = null,
        email: String = "",
        password: String = "",
    ) {
        if (message != null || email.isNotEmpty() || password.isNotEmpty()) {
            loginPromptCoordinator.show(message, email, password)
        }
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

    // Launch targets only apply to the signed-in shell (Home). Elsewhere (onboarding,
    // login...) they are dropped. On Home they are relayed once to BottomNavScreen,
    // which owns the tab NavController.
    val homeLaunchTargets = remember { Channel<LaunchTarget>(Channel.BUFFERED) }
    val homeLaunchTargetFlow = remember(homeLaunchTargets) { homeLaunchTargets.receiveAsFlow() }
    ObserveAsEvents(flow = launchTargetViewModel.events) { target ->
        val onHome = navController.currentBackStackEntry?.destination?.hasRoute<Home>() == true
        if (onHome) {
            homeLaunchTargets.trySend(target)
        } else {
            Log.d(TAG, "Dropping launch target outside Home: ${target::class.simpleName}")
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

    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navController,
        startDestination = route,
    ) {
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
                navigateToLogin = { email, password ->
                    openLogin(LoginMessage.AccountCreated, email, password)
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
                launchTargets = homeLaunchTargetFlow,
                onSignedOut = {
                    openLogin()
                },
            )
        }
    }
}
