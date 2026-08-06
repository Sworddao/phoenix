package com.sworddao.phoenix.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Onboarding : Screen("onboarding")
    data object PlayerProfile : Screen("player_profile")
    data object LearningPreferences : Screen("learning_preferences")
    data object BaoGreeting : Screen("bao_greeting/{playerName}") {
        fun createRoute(playerName: String) = "bao_greeting/$playerName"
    }
    data object QingyuanVillage : Screen("qingyuan_village/{playerName}") {
        fun createRoute(playerName: String) = "qingyuan_village/$playerName"
    }
    data object Dialogue : Screen("dialogue/{dialogueId}") {
        fun createRoute(dialogueId: String) = "dialogue/$dialogueId"
    }
    data object NpcProfile : Screen("npc_profile/{npcId}") {
        fun createRoute(npcId: String) = "npc_profile/$npcId"
    }
    data object QuestList : Screen("quest_list")
    data object QuestDetail : Screen("quest_detail/{questId}") {
        fun createRoute(questId: String) = "quest_detail/$questId"
    }
    data object WorldMap : Screen("world_map")
    data object RegionDetail : Screen("region_detail/{regionId}") {
        fun createRoute(regionId: String) = "region_detail/$regionId"
    }
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object Passport : Screen("passport")
    data object Vocabulary : Screen("vocabulary")
    data object VocabularyDetail : Screen("vocabulary_detail/{wordId}") {
        fun createRoute(wordId: String) = "vocabulary_detail/$wordId"
    }
}

data class BottomNavigationItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavigationItems = listOf(
    BottomNavigationItem(
        screen = Screen.Home,
        label = "首页",
        icon = Icons.Default.Home
    ),
    BottomNavigationItem(
        screen = Screen.Vocabulary,
        label = "词汇",
        icon = Icons.Default.MenuBook
    ),
    BottomNavigationItem(
        screen = Screen.Passport,
        label = "护照",
        icon = Icons.Default.Flight
    ),
    BottomNavigationItem(
        screen = Screen.Settings,
        label = "设置",
        icon = Icons.Default.Settings
    )
)
