package com.sworddao.phoenix.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
}

data class BottomNavigationItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavigationItems = listOf(
    BottomNavigationItem(
        screen = Screen.Home,
        label = "Home",
        icon = Icons.Default.Home
    ),
    BottomNavigationItem(
        screen = Screen.Settings,
        label = "Settings",
        icon = Icons.Default.Settings
    )
)
