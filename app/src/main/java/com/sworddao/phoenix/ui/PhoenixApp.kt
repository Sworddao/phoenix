package com.sworddao.phoenix.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.sworddao.phoenix.ui.navigation.bottomNavigationItems
import com.sworddao.phoenix.ui.screens.BaoGreetingScreen
import com.sworddao.phoenix.ui.screens.HomeScreen
import com.sworddao.phoenix.ui.screens.LearningPreferencesScreen
import com.sworddao.phoenix.ui.screens.OnboardingScreen
import com.sworddao.phoenix.ui.screens.PlayerProfileScreen
import com.sworddao.phoenix.ui.screens.QingyuanVillageScreen
import com.sworddao.phoenix.ui.screens.SettingsScreen
import com.sworddao.phoenix.ui.screens.SplashScreen
import com.sworddao.phoenix.ui.screens.WelcomeScreen
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.dialogue.ui.DialogueScreen
import com.sworddao.phoenix.feature.friendship.ui.NPCProfileScreen
import com.sworddao.phoenix.feature.npc.viewmodel.NpcViewModel
import com.sworddao.phoenix.feature.quest.ui.QuestDetailScreen
import com.sworddao.phoenix.feature.quest.ui.QuestListScreen
import com.sworddao.phoenix.feature.world.ui.WorldMapScreen
import com.sworddao.phoenix.feature.passport.ui.PassportScreen
import com.sworddao.phoenix.feature.pronunciation.ui.PronunciationScreen
import com.sworddao.phoenix.feature.listening.ui.ListeningScreen
import com.sworddao.phoenix.feature.reading.ui.ReadingScreen
import com.sworddao.phoenix.feature.writing.ui.WritingScreen
import com.sworddao.phoenix.feature.vocabulary.ui.VocabularyScreen
import com.sworddao.phoenix.feature.vocabulary.ui.VocabularyDetailScreen
import com.sworddao.phoenix.feature.vocabulary.viewmodel.VocabularyViewModel
import com.sworddao.phoenix.feature.gameplay.ui.CelebrationScreen
import com.sworddao.phoenix.feature.gameplay.viewmodel.CelebrationViewModel
import com.sworddao.phoenix.feature.gameplay.viewmodel.GameProgressViewModel
import com.sworddao.phoenix.feature.progression.ui.ProgressionScreen
import com.sworddao.phoenix.feature.review.data.ReviewType
import com.sworddao.phoenix.feature.review.ui.ReviewScreen
import com.sworddao.phoenix.feature.review.ui.ReviewSessionScreen
import com.sworddao.phoenix.ui.viewmodel.ProfileViewModel

@Composable
fun PhoenixApp(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    var playerProfile by remember { mutableStateOf(PlayerProfile()) }
    var accessibilityPrefs by remember { mutableStateOf(AccessibilityPreferences()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            bottomBar = {
                val bottomRoutes = bottomNavigationItems.map { it.screen.route }
                if (currentRoute in bottomRoutes) {
                    NavigationBar {
                        bottomNavigationItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.screen.route,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.QingyuanVillage.route)
                                        launchSingleTop = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.padding(innerPadding),
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
                val playerName = backStackEntry.arguments?.getString("playerName")
                    ?: LocalContext.current.getString(R.string.default_player_name)
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
                val playerName = backStackEntry.arguments?.getString("playerName")
                    ?: LocalContext.current.getString(R.string.default_player_name)
                QingyuanVillageScreen(
                    playerName = playerName,
                    onNavigateToDialogue = { dialogueId ->
                        navController.navigate(Screen.Dialogue.createRoute(dialogueId))
                    },
                    onNavigateToNpcProfile = { npcId ->
                        navController.navigate(Screen.NpcProfile.createRoute(npcId))
                    },
                    onNavigateToQuestList = {
                        navController.navigate(Screen.QuestList.route)
                    },
                    onNavigateToWorldMap = {
                        navController.navigate(Screen.WorldMap.route)
                    },
                    onNavigateToProgression = {
                        navController.navigate(Screen.Progression.route)
                    },
                    onNavigateToReview = {
                        navController.navigate(Screen.Review.route)
                    }
                )
            }

            composable(
                route = Screen.Dialogue.route,
                arguments = listOf(
                    navArgument("dialogueId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val dialogueId = backStackEntry.arguments?.getString("dialogueId") ?: ""
                DialogueScreen(
                    onConversationComplete = { npcId ->
                        navController.navigate(Screen.Celebration.createRoute(dialogueId, npcId)) {
                            popUpTo(Screen.Dialogue.route) { inclusive = true }
                        }
                    },
                    onPractice = {
                        navController.navigate(Screen.Pronunciation.createRoute())
                    },
                    onPracticeListening = {
                        navController.navigate(Screen.Listening.createRoute())
                    },
                    onPracticeReading = {
                        navController.navigate(Screen.Reading.createRoute())
                    },
                    onPracticeWriting = {
                        navController.navigate(Screen.Writing.createRoute())
                    }
                )
            }

            composable(
                route = Screen.Pronunciation.route,
                arguments = listOf(
                    navArgument("wordId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val wordId = backStackEntry.arguments?.getString("wordId") ?: ""
                PronunciationScreen(
                    wordId = wordId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onComplete = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Listening.route,
                arguments = listOf(
                    navArgument("wordId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val wordId = backStackEntry.arguments?.getString("wordId") ?: ""
                ListeningScreen(
                    wordId = wordId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onComplete = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Reading.route,
                arguments = listOf(
                    navArgument("wordId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val wordId = backStackEntry.arguments?.getString("wordId") ?: ""
                ReadingScreen(
                    wordId = wordId,
                    showHanzi = accessibilityPrefs.showHanzi,
                    onBack = {
                        navController.popBackStack()
                    },
                    onComplete = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Writing.route,
                arguments = listOf(
                    navArgument("wordId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val wordId = backStackEntry.arguments?.getString("wordId") ?: ""
                WritingScreen(
                    wordId = wordId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onComplete = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Celebration.route,
                arguments = listOf(
                    navArgument("dialogueId") { type = NavType.StringType },
                    navArgument("npcId") { type = NavType.StringType }
                )
            ) {
                val celebrationViewModel: CelebrationViewModel = hiltViewModel()
                val celebrationUiState by celebrationViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    celebrationViewModel.loadResults()
                }

                CelebrationScreen(
                    uiState = celebrationUiState,
                    onContinue = {
                        navController.popBackStack(Screen.QingyuanVillage.route, false)
                    }
                )
            }

            composable(
                route = Screen.NpcProfile.route,
                arguments = listOf(
                    navArgument("npcId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val npcId = backStackEntry.arguments?.getString("npcId") ?: ""
                val npcViewModel: NpcViewModel = hiltViewModel()
                val npcUiState by npcViewModel.uiState.collectAsState()
                val npc = npcUiState.npcs.find { it.id == npcId }

                if (npc != null) {
                    NPCProfileScreen(
                        npc = npc,
                        onStartConversation = { dialogueId ->
                            navController.navigate(Screen.Dialogue.createRoute(dialogueId))
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(Screen.QuestList.route) {
                QuestListScreen(
                    onQuestClick = { questId ->
                        navController.navigate(Screen.QuestDetail.createRoute(questId))
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.QuestDetail.route,
                arguments = listOf(
                    navArgument("questId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val questId = backStackEntry.arguments?.getString("questId") ?: ""
                QuestDetailScreen(
                    questId = questId,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.WorldMap.route) {
                WorldMapScreen(
                    onRegionClick = { regionId ->
                        // Future: navigate to region detail
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Passport.route) {
                PassportScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Vocabulary.route) {
                val vocabularyViewModel: VocabularyViewModel = hiltViewModel()
                val vocabUiState by vocabularyViewModel.uiState.collectAsState()
                VocabularyScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onWordClick = { word ->
                        navController.navigate(Screen.VocabularyDetail.createRoute(word.id))
                    },
                    uiState = vocabUiState,
                    onSearch = { vocabularyViewModel.search(it) },
                    onCategoryFilter = { vocabularyViewModel.filterByCategory(it) },
                    onMasteryFilter = { vocabularyViewModel.filterByMastery(it) },
                    onToggleFavorites = { vocabularyViewModel.toggleFavoritesOnly() },
                    onToggleRecentlyLearned = { vocabularyViewModel.toggleRecentlyLearned() },
                    onToggleMastered = { vocabularyViewModel.toggleMasteredOnly() },
                )
            }

            composable(
                route = Screen.VocabularyDetail.route,
                arguments = listOf(
                    navArgument("wordId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val wordId = backStackEntry.arguments?.getString("wordId") ?: ""
                val vocabularyViewModel: VocabularyViewModel = hiltViewModel()
                val vocabUiState by vocabularyViewModel.uiState.collectAsState()
                val word = vocabUiState.words.find { it.id == wordId }
                if (word != null) {
                    VocabularyDetailScreen(
                        word = word,
                        onBack = { navController.popBackStack() },
                        onToggleFavorite = { vocabularyViewModel.toggleFavorite(it) },
                        onUpdateMastery = { id, mastery -> vocabularyViewModel.updateMastery(id, mastery) },
                        onPractice = { wordId ->
                            navController.navigate(Screen.Pronunciation.createRoute(wordId))
                        },
                        onPracticeListening = { wordId ->
                            navController.navigate(Screen.Listening.createRoute(wordId))
                        },
                        onPracticeReading = { wordId ->
                            navController.navigate(Screen.Reading.createRoute(wordId))
                        },
                        onPracticeWriting = { wordId ->
                            navController.navigate(Screen.Writing.createRoute(wordId))
                        },
                    )
                }
            }

            composable(Screen.Home.route) {
                val gameProgressViewModel: GameProgressViewModel = hiltViewModel()
                val gameProgressUiState by gameProgressViewModel.uiState.collectAsState()
                HomeScreen(
                    vocabularyCount = gameProgressUiState.gameProgress.totalWordsDiscovered,
                    xp = gameProgressUiState.sessionSummary.totalXpEarned
                )
            }

            composable(Screen.Progression.route) {
                ProgressionScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Review.route) {
                ReviewScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenSession = { type ->
                        navController.navigate(Screen.ReviewSession.createRoute(type))
                    }
                )
            }

            composable(
                route = Screen.ReviewSession.route,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType }
                )
            ) {
                val typeName = it.arguments?.getString("type") ?: ReviewType.DAILY_REVIEW.name
                ReviewSessionScreen(
                    type = runCatching { ReviewType.valueOf(typeName) }
                        .getOrDefault(ReviewType.DAILY_REVIEW),
                    onBack = {
                        navController.popBackStack()
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
}
