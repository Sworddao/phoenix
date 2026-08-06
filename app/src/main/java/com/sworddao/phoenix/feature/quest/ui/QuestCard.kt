package com.sworddao.phoenix.feature.quest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.feature.quest.data.Quest
import com.sworddao.phoenix.feature.quest.data.QuestCategory
import com.sworddao.phoenix.feature.quest.data.QuestDifficulty
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.quest.data.QuestType

@Composable
fun QuestCard(
    quest: Quest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (quest.status) {
                QuestStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
                QuestStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                QuestStatus.AVAILABLE -> MaterialTheme.colorScheme.surface
                QuestStatus.LOCKED -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (quest.status == QuestStatus.ACTIVE) 4.dp else 2.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (quest.status) {
                        QuestStatus.LOCKED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                QuestStatusBadge(status = quest.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = quest.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuestDifficultyChip(difficulty = quest.difficulty)
                    Spacer(modifier = Modifier.width(8.dp))
                    QuestCategoryChip(category = quest.category)
                }

                if (quest.status == QuestStatus.ACTIVE) {
                    QuestProgressIndicator(progress = quest.progress)
                }
            }

            if (quest.objectives.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "目标: ${quest.completedObjectives}/${quest.totalObjectives}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
fun QuestStatusBadge(
    status: QuestStatus,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor, text) = when (status) {
        QuestStatus.LOCKED -> Triple(
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            "锁定",
        )
        QuestStatus.AVAILABLE -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "可接",
        )
        QuestStatus.ACTIVE -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "进行中",
        )
        QuestStatus.COMPLETED -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "已完成",
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

@Composable
fun QuestDifficultyChip(
    difficulty: QuestDifficulty,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor) = when (difficulty) {
        QuestDifficulty.EASY -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        QuestDifficulty.MEDIUM -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        QuestDifficulty.HARD -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        QuestDifficulty.EXPERT -> Pair(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = difficulty.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

@Composable
fun QuestCategoryChip(
    category: QuestCategory,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun QuestTypeIcon(
    type: QuestType,
    modifier: Modifier = Modifier,
) {
    val icon = when (type) {
        QuestType.CONVERSATION -> "💬"
        QuestType.LISTENING -> "👂"
        QuestType.SPEAKING -> "🗣"
        QuestType.EXPLORATION -> "🗺"
        QuestType.MEMORY -> "🧠"
        QuestType.PRONUNCIATION -> "🗣"
        QuestType.STORY -> "📖"
        QuestType.MINI_GAME -> "🎮"
        QuestType.PHOTOGRAPHY -> "📷"
        QuestType.COLLECTING -> "📦"
        QuestType.DAILY -> "📅"
    }

    Text(
        text = icon,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}

@Composable
fun QuestProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
