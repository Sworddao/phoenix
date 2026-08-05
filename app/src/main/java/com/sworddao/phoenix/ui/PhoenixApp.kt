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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sworddao.phoenix.data.model.AccessibilityPreferences
import com.sworddao.phoenix.data.model.PlayerProfile
import com.sworddao.phoenix.ui.navigation.Screen
import com.sworddao.phoenix.ui.screens.BaoGreetingScreen
import com.sworddao.phoenix.ui.screens.HomeScreen
import com.sworddao.phoenix.ui.screens.LearningPreferencesScreen
import com.sworddao.phoenix.ui.screens.OnboardingScreen
import com.sworddao.phoenix.ui.screens.PlayerProfileScreen
import com.sworddao.phoenix.ui.screens.QingyuanVillageScreen
import com.sworddao.phoenix.ui.screens.SettingsScreen
import com.sworddao.phoenix.ui.screens.SplashScreen
import com.sworddao.phoenix.ui.screens.WelcomeScreen
import com.sworddao.phoenix.ui.viewmodel.ProfileViewModel

@Composable
fun PhoenixApp(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var playerProfile by remember { mutableStateOf(PlayerProfile()) }
    var accessibilityPrefs by remember { mutableStateOf(AccessibilityPreferences()) }

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
                WelcomeScreen(
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onOnboardingComplete = {
                        navController.navigate(Screen.PlayerProfile.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.PlayerProfile.route) {
                PlayerProfileScreen(
                    onProfileCreated = { profile ->
                        playerProfile = profile
                        navController.navigate(Screen.LearningPreferences.route) {
                            popUpTo(Screen.PlayerProfile.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.LearningPreferences.route) {
                LearningPreferencesScreen(
                    onPreferencesSaved = { prefs ->
                        accessibilityPrefs = prefs
                        viewModel.saveProfile(playerProfile)
                        viewModel.saveAccessibilityPreferences(prefs)
                        navController.navigate(Screen.BaoGreeting.createRoute(playerProfile.displayName)) {
                            popUpTo(Screen.LearningPreferences.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.BaoGreeting.route,
                arguments = listOf(
                    navArgument("playerName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val playerName = backStackEntry.arguments?.getString("playerName") ?: "Friend"
                BaoGreetingScreen(
                    playerName = playerName,
                    onContinue = {
                        navController.navigate(Screen.QingyuanVillage.createRoute(playerName)) {
                            popUpTo(Screen.BaoGreeting.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.QingyuanVillage.route,
                arguments = listOf(
                    navArgument("playerName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val playerName = backStackEntry.arguments?.getString("playerName") ?: "Friend"
                QingyuanVillageScreen(
                    playerName = playerName
                )
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
