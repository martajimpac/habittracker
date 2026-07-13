package com.marta.habittracker.view.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.marta.habittracker.view.screens.auth.login.LoginScreen
import com.marta.habittracker.view.screens.auth.register.RegisterScreen
import com.marta.habittracker.view.screens.bottom_nav_screen.BottomNavScreen

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