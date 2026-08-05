package com.sworddao.phoenix.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sworddao.phoenix.ui.navigation.Screen
import com.sworddao.phoenix.ui.screens.HomeScreen
import com.sworddao.phoenix.ui.screens.SettingsScreen
import com.sworddao.phoenix.ui.screens.SplashScreen
import com.sworddao.phoenix.ui.screens.WelcomeScreen

@Composable
fun PhoenixApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            enterTransition = {
                fadeIn(animationSpec = tween(500)) + slideInHorizontally(
                    animationSpec = tween(500),
                    initialOffsetX = { fullWidth -> fullWidth / 4 }
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(500)) + slideOutHorizontally(
                    animationSpec = tween(500),
                    targetOffsetX = { fullWidth -> -fullWidth / 4 }
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(500)) + slideInHorizontally(
                    animationSpec = tween(500),
                    initialOffsetX = { fullWidth -> -fullWidth / 4 }
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(500)) + slideOutHorizontally(
                    animationSpec = tween(500),
                    targetOffsetX = { fullWidth -> fullWidth / 4 }
                )
            }
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToWelcome = {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Welcome.route) {
                WelcomeScreen()
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
