package com.sworddao.phoenix.feature.quest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.feature.quest.data.ObjectiveType
import com.sworddao.phoenix.feature.quest.data.QuestObjective

@Composable
fun QuestObjectiveList(
    objectives: List<QuestObjective>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "任务目标",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        objectives.forEach { objective ->
            QuestObjectiveItem(objective = objective)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun QuestObjectiveItem(
    objective: QuestObjective,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (objective.isComplete) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = getObjectiveIcon(objective.type),
                contentDescription = null,
                tint = if (objective.isComplete) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(24.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = objective.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (objective.isComplete) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )

                if (objective.targetCount > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { objective.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (objective.isComplete) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        text = "${objective.currentCount}/${objective.targetCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }

            if (objective.isComplete) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "已完成",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            } else if (objective.optional) {
                Text(
                    text = "可选",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun getObjectiveIcon(type: ObjectiveType): ImageVector = when (type) {
    ObjectiveType.TALK_TO_NPC -> Icons.Filled.Person
    ObjectiveType.COMPLETE_DIALOGUE -> Icons.Filled.Person
    ObjectiveType.VISIT_LOCATION -> Icons.Filled.LocationOn
    ObjectiveType.COLLECT_ITEM -> Icons.Filled.Star
    ObjectiveType.LEARN_VOCABULARY -> Icons.Filled.Star
    ObjectiveType.PRACTICE_SPEAKING -> Icons.Filled.Person
    ObjectiveType.LISTEN_TO_AUDIO -> Icons.Filled.Person
    ObjectiveType.READ_CHARACTERS -> Icons.Filled.MenuBook
    ObjectiveType.WRITE_CHARACTERS -> Icons.Filled.Edit
    ObjectiveType.PHOTOGRAPH -> Icons.Filled.Star
    ObjectiveType.DEFEAT_BOSS -> Icons.Filled.Star
    ObjectiveType.FIND_SECRET -> Icons.Filled.Star
    ObjectiveType.ESCORT_NPC -> Icons.Filled.Person
    ObjectiveType.DELIVER_ITEM -> Icons.Filled.Star
    ObjectiveType.EARN_FRIENDSHIP_POINTS -> Icons.Filled.Person
    ObjectiveType.COMPLETE_MINI_GAME -> Icons.Filled.Star
}
