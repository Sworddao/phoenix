package com.sworddao.phoenix.feature.passport.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.feature.passport.data.PassportRegion
import com.sworddao.phoenix.feature.passport.data.StampRarity

@Composable
fun PassportStampCard(
    region: PassportRegion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (region.stampEarned) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (region.isDiscovered) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (region.stampEarned) 4.dp else 2.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Stamp circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (region.stampEarned) {
                            Color(region.stampRarity.color).copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (region.stampEarned) {
                    Text(
                        text = "📮",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                } else if (region.isDiscovered) {
                    Text(
                        text = "📍",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                } else {
                    Text(
                        text = "🔒",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = region.regionName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (region.isDiscovered) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        },
                    )
                    if (region.stampEarned) {
                        StampRarityBadge(rarity = region.stampRarity)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = region.regionNameCn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )

                if (region.isDiscovered) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "探索: ${(region.completionPercentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "收集: ${region.collectiblesFound}/${region.collectiblesTotal}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "词汇: ${region.vocabularyLearned}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Text(
                            text = "朋友: ${region.friendshipsMade}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Text(
                            text = "任务: ${region.questsCompleted}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StampRarityBadge(
    rarity: StampRarity,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(rarity.color).copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = rarity.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = Color(rarity.color),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun RegionCompletionDialog(
    region: PassportRegion,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "区域完成",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                Text(
                    text = "恭喜完成 ${region.regionName} (${region.regionNameCn})!",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("词汇学习", style = MaterialTheme.typography.bodyMedium)
                    Text("${region.vocabularyLearned}个", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("友谊建立", style = MaterialTheme.typography.bodyMedium)
                    Text("${region.friendshipsMade}位", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("任务完成", style = MaterialTheme.typography.bodyMedium)
                    Text("${region.questsCompleted}个", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("收集品发现", style = MaterialTheme.typography.bodyMedium)
                    Text("${region.collectiblesFound}/${region.collectiblesTotal}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("太棒了！")
            }
        },
    )
}
