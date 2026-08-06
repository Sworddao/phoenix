package com.sworddao.phoenix.feature.discovery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sworddao.phoenix.feature.discovery.data.AnimationPhase
import com.sworddao.phoenix.feature.discovery.data.DiscoveryReward
import com.sworddao.phoenix.feature.discovery.data.DiscoverySourceType
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import kotlinx.coroutines.delay

@Composable
fun VocabularyDiscoveryDialog(
    word: VocabularyWord,
    source: DiscoverySourceType,
    sourceName: String,
    isFirstDiscovery: Boolean,
    reward: DiscoveryReward?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var animationPhase by remember { mutableStateOf(AnimationPhase.WORD_APPEARING) }
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        )
        animationPhase = AnimationPhase.WORD_DISPLAYING
        delay(1500)
        animationPhase = AnimationPhase.REWARD_SHOWING
        delay(1000)
        animationPhase = AnimationPhase.COMPLETING
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.scale(scale.value),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (isFirstDiscovery) {
                                    listOf(Color(0xFFFFD700), Color(0xFFFFA000))
                                } else {
                                    listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isFirstDiscovery) Icons.Default.NewReleases else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isFirstDiscovery) "New Word Discovered!" else "Word Reviewed!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "via $sourceName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = word.pinyin,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = word.english,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )

                if (word.exampleSentence.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = word.exampleSentence,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = word.exampleTranslation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (animationPhase == AnimationPhase.REWARD_SHOWING && reward != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Rewards",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (reward.xp > 0) {
                                    RewardChip(
                                        icon = Icons.Default.Star,
                                        label = "+${reward.xp} XP",
                                        color = Color(0xFFFFD700),
                                    )
                                }
                                if (reward.friendshipXp > 0) {
                                    RewardChip(
                                        icon = Icons.Default.Favorite,
                                        label = "+${reward.friendshipXp} Friendship",
                                        color = Color(0xFFE91E63),
                                    )
                                }
                                if (reward.streakBonus > 0) {
                                    RewardChip(
                                        icon = Icons.Default.TrendingUp,
                                        label = "+${reward.streakBonus} Streak",
                                        color = Color(0xFFFF9800),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Continue")
            }
        },
    )
}

@Composable
private fun RewardChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun NewWordCard(
    word: VocabularyWord,
    source: DiscoverySourceType,
    sourceName: String,
    discoveredAt: Long,
    isFirstDiscovery: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFirstDiscovery) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFirstDiscovery) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = word.pinyin.first().toString(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.pinyin,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = word.english,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "via $sourceName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isFirstDiscovery) {
                Icon(
                    imageVector = Icons.Default.NewReleases,
                    contentDescription = "New",
                    tint = Color(0xFFFFD700),
                )
            }
        }
    }
}

@Composable
fun DiscoveryTimeline(
    discoveries: List<com.sworddao.phoenix.feature.discovery.data.VocabularyDiscovery>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(discoveries) { discovery ->
            NewWordCard(
                word = discovery.word ?: return@items,
                source = discovery.source,
                sourceName = discovery.sourceName,
                discoveredAt = discovery.discoveredAt,
                isFirstDiscovery = discovery.isFirstDiscovery,
            )
        }
    }
}

@Composable
fun DiscoverySummaryCard(
    totalDiscovered: Int,
    todayDiscovered: Int,
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Discovery Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SummaryItem(
                    value = totalDiscovered.toString(),
                    label = "Total",
                    color = MaterialTheme.colorScheme.primary,
                )
                SummaryItem(
                    value = todayDiscovered.toString(),
                    label = "Today",
                    color = Color(0xFF4CAF50),
                )
                SummaryItem(
                    value = streakDays.toString(),
                    label = "Streak",
                    color = Color(0xFFFF9800),
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun RewardBanner(
    xp: Int,
    friendshipXp: Int,
    streakBonus: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (xp > 0) {
                RewardChip(
                    icon = Icons.Default.Star,
                    label = "+$xp XP",
                    color = Color(0xFFFFD700),
                )
            }
            if (friendshipXp > 0) {
                RewardChip(
                    icon = Icons.Default.Favorite,
                    label = "+$friendshipXp FP",
                    color = Color(0xFFE91E63),
                )
            }
            if (streakBonus > 0) {
                RewardChip(
                    icon = Icons.Default.TrendingUp,
                    label = "+$streakBonus Streak",
                    color = Color(0xFFFF9800),
                )
            }
        }
    }
}

@Composable
fun DailyDiscoveryCard(
    date: String,
    count: Int,
    isStreak: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isStreak) {
                Color(0xFFE8F5E9)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "$count word${if (count != 1) "s" else ""} discovered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isStreak) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Streak",
                    tint = Color(0xFF4CAF50),
                )
            }
        }
    }
}
