package com.aristidevs.habittracker.view.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aristidevs.habittracker.view.auth.login.LoginScreen
import com.aristidevs.habittracker.view.auth.register.RegisterScreen
import com.aristidevs.habittracker.view.bottom_nav_screen.BottomNavScreen

@Composable
fun NavigationWrapper() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Home) {
        composable<Login> {
            LoginScreen(
                navigateToRegister = { navController.navigate(Register) },
                navigateToHome = { navController.navigate(Home){
                    popUpTo(0)
                } })
        }

        composable<Register> {
            RegisterScreen(navigateBack = { navController.popBackStack() })
        }

        composable<Home> {
            BottomNavScreen()
        }

    }
}