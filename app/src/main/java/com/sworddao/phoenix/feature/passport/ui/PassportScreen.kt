package com.sworddao.phoenix.feature.passport.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sworddao.phoenix.feature.passport.data.Collectible
import com.sworddao.phoenix.feature.passport.data.CollectibleCategory
import com.sworddao.phoenix.feature.passport.data.PassportRegion
import com.sworddao.phoenix.feature.passport.viewmodel.PassportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportScreen(
    onBack: () -> Unit,
    viewModel: PassportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "护照",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PassportStatsCard(
                        stamps = uiState.passport?.totalStamps ?: 0,
                        collectibles = uiState.stats?.collectedItems ?: 0,
                        discoveries = uiState.passport?.totalDiscoveries ?: 0,
                        completionPercentage = uiState.stats?.completionPercentage ?: 0f
                    )
                }

                item {
                    SectionTitle(title = "收藏进度")
                    CollectionProgressCard(
                        collectionProgress = uiState.collectionProgress,
                        onShowCollectionGrid = { viewModel.showCollectionGrid() }
                    )
                }

                item {
                    SectionTitle(title = "区域护照")
                }

                items(uiState.regions) { region ->
                    PassportStampCard(
                        region = region,
                        onClick = { viewModel.selectRegion(region) }
                    )
                }

                if (uiState.timeline.isNotEmpty()) {
                    item {
                        SectionTitle(title = "旅行日志")
                    }

                    items(uiState.timeline) { event ->
                        TimelineEventCard(event = event)
                    }
                }

                if (uiState.achievements.isNotEmpty()) {
                    item {
                        SectionTitle(title = "成就")
                    }

                    items(uiState.achievements) { achievement ->
                        AchievementCard(achievement = achievement)
                    }
                }
            }
        }
    }

    uiState.selectedRegion?.let { region ->
        RegionDetailDialog(
            region = region,
            onDismiss = { viewModel.clearSelectedRegion() },
            onComplete = { viewModel.completeRegion(region.regionId) },
            onEarnStamp = { viewModel.earnStamp(region.regionId) }
        )
    }

    uiState.selectedCollectible?.let { collectible ->
        CollectibleDetailDialog(
            collectible = collectible,
            onDismiss = { viewModel.clearSelectedCollectible() }
        )
    }

    if (uiState.showCollectionGrid) {
        CollectionGridDialog(
            collectibles = uiState.collectibles,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { /* Filter by category */ },
            onDismiss = { viewModel.hideCollectionGrid() },
            onCollectibleClick = { viewModel.selectCollectible(it) }
        )
    }

    uiState.error?.let { error ->
        ErrorDialog(
            message = error,
            onDismiss = { viewModel.clearError() }
        )
    }
}

@Composable
private fun PassportStatsCard(
    stamps: Int,
    collectibles: Int,
    discoveries: Int,
    completionPercentage: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "旅行统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stamps.toString(),
                    label = "印章",
                    icon = Icons.Default.EmojiEvents
                )
                StatItem(
                    value = collectibles.toString(),
                    label = "收藏品",
                    icon = Icons.Default.Collections
                )
                StatItem(
                    value = discoveries.toString(),
                    label = "发现",
                    icon = Icons.Default.Explore
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { completionPercentage / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "完成度 ${completionPercentage.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun CollectionProgressCard(
    collectionProgress: com.sworddao.phoenix.feature.passport.data.CollectionProgress?,
    onShowCollectionGrid: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onShowCollectionGrid
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "收藏品进度",
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (collectionProgress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${collectionProgress.collectedCount} / ${collectionProgress.totalCollectibles} 已收集",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimelineEventCard(
    event: com.sworddao.phoenix.feature.passport.data.DiscoveryEvent
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getEventIcon(event.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: com.sworddao.phoenix.feature.passport.data.AchievementProgress
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (achievement.isUnlocked) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (achievement.isUnlocked)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!achievement.isUnlocked) {
                Text(
                    text = "${achievement.currentCount}/${achievement.requiredCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RegionDetailDialog(
    region: PassportRegion,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onEarnStamp: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = region.regionName)
                Text(
                    text = region.regionNameCn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "探索进度",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { region.completionPercentage / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${region.completionPercentage.toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "统计",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "词汇: ${region.vocabularyLearned}")
                Text(text = "友谊: ${region.friendshipsMade}")
                Text(text = "任务: ${region.questsCompleted}")
                Text(text = "收藏品: ${region.collectiblesFound}/${region.collectiblesTotal}")
            }
        },
        confirmButton = {
            Row {
                if (!region.stampEarned) {
                    TextButton(onClick = onEarnStamp) {
                        Text("获取印章")
                    }
                }
                if (region.completionPercentage < 100f) {
                    TextButton(onClick = onComplete) {
                        Text("完成区域")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun CollectibleDetailDialog(
    collectible: Collectible,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = collectible.name)
                Text(
                    text = collectible.nameCn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                Text(
                    text = collectible.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "类别: ${collectible.category.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "稀有度: ${collectible.rarity.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "来源: ${collectible.source.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!collectible.culturalNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = collectible.culturalNote,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun CollectionGridDialog(
    collectibles: List<Collectible>,
    selectedCategory: CollectibleCategory?,
    onCategorySelected: (CollectibleCategory?) -> Unit,
    onDismiss: () -> Unit,
    onCollectibleClick: (Collectible) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "收藏品图鉴") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("全部") }
                    )
                    CollectibleCategory.entries.take(4).forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { onCategorySelected(category) },
                            label = { Text(category.displayName) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        collectibles.filter { collectible ->
                            selectedCategory == null || collectible.category == selectedCategory
                        }
                    ) { collectible ->
                        CollectibleGridItem(
                            collectible = collectible,
                            onClick = { onCollectibleClick(collectible) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun CollectibleGridItem(
    collectible: Collectible,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (collectible.isCollected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = collectible.category.icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (collectible.isCollected) collectible.nameCn else "???",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("错误") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

private fun getEventIcon(eventType: com.sworddao.phoenix.feature.passport.data.EntryType) =
    when (eventType) {
        com.sworddao.phoenix.feature.passport.data.EntryType.REGION_DISCOVERED -> Icons.Default.Explore
        com.sworddao.phoenix.feature.passport.data.EntryType.REGION_COMPLETED -> Icons.Default.CheckCircle
        com.sworddao.phoenix.feature.passport.data.EntryType.STAMP_EARNED -> Icons.Default.EmojiEvents
        com.sworddao.phoenix.feature.passport.data.EntryType.COLLECTIBLE_FOUND -> Icons.Default.Collections
        com.sworddao.phoenix.feature.passport.data.EntryType.QUEST_COMPLETED -> Icons.Default.Assignment
        com.sworddao.phoenix.feature.passport.data.EntryType.NPC_MET -> Icons.Default.Person
        com.sworddao.phoenix.feature.passport.data.EntryType.FRIENDSHIP_LEVEL_UP -> Icons.Default.Group
        com.sworddao.phoenix.feature.passport.data.EntryType.DIALOGUE_COMPLETED -> Icons.Default.Chat
        com.sworddao.phoenix.feature.passport.data.EntryType.VOCABULARY_LEARNED -> Icons.Default.MenuBook
        com.sworddao.phoenix.feature.passport.data.EntryType.SPEAKING_PRACTICE -> Icons.Default.Mic
        com.sworddao.phoenix.feature.passport.data.EntryType.ACHIEVEMENT_UNLOCKED -> Icons.Default.Star
    }
