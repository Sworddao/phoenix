package com.sworddao.phoenix.feature.quest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.quest.viewmodel.QuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailScreen(
    questId: String,
    onBackClick: () -> Unit,
    viewModel: QuestViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(questId) {
        val quest = uiState.quests.find { it.id == questId }
        quest?.let { viewModel.selectQuest(it) }
    }

    val quest = uiState.selectedQuest

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quest?.title ?: "任务详情") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelectedQuest()
                        onBackClick()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (quest == null) {
            Text(
                text = "加载中...",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Header with type and difficulty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuestTypeIcon(type = quest.type)
                Row {
                    QuestDifficultyChip(difficulty = quest.difficulty)
                    Spacer(modifier = Modifier.padding(4.dp))
                    QuestCategoryChip(category = quest.category)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status badge
            QuestStatusBadge(status = quest.status)

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = quest.description,
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Objectives
            QuestObjectiveList(objectives = quest.objectives)

            Spacer(modifier = Modifier.height(24.dp))

            // Rewards
            QuestRewardCard(rewards = quest.rewards)

            Spacer(modifier = Modifier.height(24.dp))

            // Progress (if active)
            if (quest.status == QuestStatus.ACTIVE) {
                Text(
                    text = "进度: ${(quest.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action buttons
            when (quest.status) {
                QuestStatus.AVAILABLE -> {
                    FilledTonalButton(
                        onClick = { viewModel.startQuest(quest.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                        Text(
                            text = "开始任务",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                QuestStatus.ACTIVE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.abandonQuest(quest.id) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("放弃")
                        }
                        FilledTonalButton(
                            onClick = { viewModel.completeQuest(quest.id) },
                            modifier = Modifier.weight(1f),
                            enabled = quest.isComplete,
                        ) {
                            Text("完成")
                        }
                    }
                }
                QuestStatus.COMPLETED -> {
                    Text(
                        text = "任务已完成",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                QuestStatus.LOCKED -> {
                    Text(
                        text = "任务未解锁",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Completion dialogue (if available)
            quest.completionDialogue?.let { dialogue ->
                if (quest.status == QuestStatus.COMPLETED) {
                    Text(
                        text = "完成对话",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        text = dialogue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            // Error display
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            // Completion dialog
            if (uiState.showCompletionDialog) {
                val completedResult = uiState.completedQuestResult
                if (completedResult is com.sworddao.phoenix.feature.quest.data.QuestResult.QuestCompleted) {
                    QuestCompletionDialog(
                        quest = completedResult.quest,
                        rewards = completedResult.rewards,
                        onDismiss = { viewModel.dismissCompletionDialog() },
                    )
                }
            }
        }
    }
}
