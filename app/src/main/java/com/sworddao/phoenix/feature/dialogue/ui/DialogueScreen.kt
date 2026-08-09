package com.sworddao.phoenix.feature.dialogue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.dialogue.data.DialogueNodeType
import com.sworddao.phoenix.feature.dialogue.viewmodel.DialogueViewModel

@Composable
fun DialogueScreen(
    onConversationComplete: (npcId: String) -> Unit,
    onPractice: (() -> Unit)? = null,
    onPracticeListening: (() -> Unit)? = null,
    onPracticeReading: (() -> Unit)? = null,
    onPracticeWriting: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: DialogueViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.history.size) {
        if (uiState.history.isNotEmpty()) {
            listState.animateScrollToItem(uiState.history.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics {
                contentDescription = context.getString(R.string.dialogue_screen_accessibility)
            }
    ) {
        uiState.dialogue?.let { dialogue ->
            val npc = com.sworddao.phoenix.feature.npc.data.Npc(
                id = dialogue.npcId,
                displayName = dialogue.title.split(" ").drop(1).joinToString(" "),
                occupation = "Village Resident",
                personality = "",
                currentLocation = "",
                schedule = com.sworddao.phoenix.feature.npc.data.NpcSchedule(emptyList()),
                avatarEmoji = "\uD83D\uDC75"
            )
            DialogueHeader(npc = npc)
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState
            ) {
                items(uiState.history) { entry ->
                    DialogueBubble(
                        entry = entry,
                        onReadingPractice = if (onPracticeReading != null) {
                            { _ -> onPracticeReading() }
                        } else {
                            null
                        }
                    )
                }
            }

            if (uiState.isConversationComplete) {
                val npcId = uiState.dialogue?.npcId ?: ""
                ConversationCompleteCard(
                    history = uiState.history,
                    onContinue = { onConversationComplete(npcId) },
                    onPractice = if (uiState.isPracticeAvailable) {
                        { onPractice?.invoke() ?: onConversationComplete(npcId) }
                    } else {
                        null
                    },
                    onPracticeListening = if (uiState.isListeningPracticeAvailable) {
                        { onPracticeListening?.invoke() ?: onConversationComplete(npcId) }
                    } else {
                        null
                    },
                    onPracticeReading = if (uiState.isReadingPracticeAvailable) {
                        { onPracticeReading?.invoke() ?: onConversationComplete(npcId) }
                    } else {
                        null
                    },
                    onPracticeWriting = if (uiState.isWritingPracticeAvailable) {
                        { onPracticeWriting?.invoke() ?: onConversationComplete(npcId) }
                    } else {
                        null
                    }
                )
            } else if (uiState.availableChoices.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dialogue_choose_response),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    uiState.availableChoices.forEach { choice ->
                        PlayerChoiceCard(
                            choice = choice,
                            onClick = { viewModel.selectChoice(choice.id) }
                        )
                    }
                }
            } else if (uiState.currentNode?.nextNodeId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.advanceDialogue() }
                    ) {
                        Text(
                            text = stringResource(R.string.dialogue_continue),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
