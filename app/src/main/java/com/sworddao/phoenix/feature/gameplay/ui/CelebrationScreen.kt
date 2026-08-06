package com.sworddao.phoenix.feature.gameplay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.dialogue.data.ActionType
import com.sworddao.phoenix.feature.dialogue.data.DialogueAction
import com.sworddao.phoenix.feature.dialogue.viewmodel.ProcessedAction
import kotlinx.coroutines.delay

data class CelebrationUiState(
    val npcId: String = "",
    val npcName: String = "",
    val dialogueTitle: String = "",
    val processedActions: List<ProcessedAction> = emptyList(),
    val xpEarned: Int = 0,
    val baoMessage: String = "",
    val milestonesUnlocked: List<String> = emptyList()
)

@Composable
fun CelebrationScreen(
    uiState: CelebrationUiState,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(300)
        showContent = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .semantics {
                contentDescription = context.getString(R.string.celebration_screen_accessibility)
            }
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(500)) +
                    slideInVertically(animationSpec = tween(500)) { it / 2 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.celebration_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = uiState.npcName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.processedActions.isNotEmpty()) {
                    CelebrationActionsSection(actions = uiState.processedActions)
                }

                if (uiState.xpEarned > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.celebration_xp_earned, uiState.xpEarned),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (uiState.milestonesUnlocked.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.celebration_milestones_unlocked),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.milestonesUnlocked.forEach { milestone ->
                                Text(
                                    text = "• $milestone",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                if (uiState.baoMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🐾 Bao",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.baoMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = stringResource(R.string.celebration_continue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CelebrationActionsSection(
    actions: List<ProcessedAction>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.filter { it.success }.forEach { action ->
            CelebrationActionCard(action = action)
        }
    }
}

@Composable
private fun CelebrationActionCard(
    action: ProcessedAction,
    modifier: Modifier = Modifier
) {
    val (icon, title, description) = when (action.type) {
        ActionType.ADD_FRIENDSHIP_XP -> Triple(
            Icons.Default.Favorite,
            stringResource(R.string.celebration_friendship_title),
            stringResource(R.string.celebration_friendship_description, action.value)
        )
        ActionType.UNLOCK_VOCABULARY -> {
            val wordCount = action.value.split(",").size
            Triple(
                Icons.Default.MenuBook,
                stringResource(R.string.celebration_vocabulary_title),
                stringResource(R.string.celebration_vocabulary_description, wordCount)
            )
        }
        ActionType.COMPLETE_QUEST -> Triple(
            Icons.Default.TaskAlt,
            stringResource(R.string.celebration_quest_title),
            stringResource(R.string.celebration_quest_description)
        )
        ActionType.PRACTICE_SPEAKING -> Triple(
            Icons.Default.Star,
            stringResource(R.string.pronunciation_practice_button),
            stringResource(R.string.celebration_speaking_description)
        )
        ActionType.PRACTICE_LISTENING -> Triple(
            Icons.Default.Headphones,
            stringResource(R.string.listening_practice_button),
            stringResource(R.string.celebration_listening_description)
        )
        ActionType.GIVE_ITEM -> Triple(
            Icons.Default.Star,
            stringResource(R.string.celebration_item_title),
            stringResource(R.string.celebration_item_description)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
