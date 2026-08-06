package com.sworddao.phoenix.feature.world.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sworddao.phoenix.feature.world.viewmodel.WorldViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldMapScreen(
    onRegionClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: WorldViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("世界地图") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Current location banner
            uiState.currentRegion?.let { region ->
                CurrentLocationBanner(region = region)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Exploration progress
            ExplorationProgressCard(
                completionPercentage = uiState.explorationProgress.completionPercentage,
                regionsCompleted = uiState.explorationProgress.completedRegions,
                totalRegions = uiState.explorationProgress.totalRegions,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Region list
            if (uiState.isLoading) {
                Text(
                    text = "加载中...",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.regions) { region ->
                        RegionCard(
                            region = region,
                            onClick = {
                                viewModel.selectRegion(region)
                            },
                        )
                    }
                }
            }
        }
    }

    // Region detail dialog
    if (uiState.showRegionDetail && uiState.selectedRegion != null) {
        RegionDetailDialog(
            region = uiState.selectedRegion!!,
            onDismiss = { viewModel.clearSelectedRegion() },
            onTravel = { viewModel.startTravel(uiState.selectedRegion!!) },
        )
    }

    // Travel confirmation dialog
    if (uiState.showTravelDialog && uiState.travelTarget != null) {
        TravelConfirmDialog(
            targetRegion = uiState.travelTarget!!,
            onConfirm = { viewModel.confirmTravel(uiState.travelTarget!!.id) },
            onDismiss = { viewModel.cancelTravel() },
        )
    }

    // Newly unlocked regions notification
    if (uiState.newlyUnlockedRegions.isNotEmpty()) {
        val unlockedNames = uiState.newlyUnlockedRegions.map { regionId ->
            uiState.regions.find { it.id == regionId }?.name ?: regionId
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnlockedNotification() },
            title = { Text("新区域解锁！") },
            text = {
                Text("已解锁: ${unlockedNames.joinToString(", ")}")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissUnlockedNotification() }) {
                    Text("太好了！")
                }
            },
        )
    }

    // Error display
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("错误") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("确定")
                }
            },
        )
    }
}

@Composable
fun RegionDetailDialog(
    region: com.sworddao.phoenix.feature.world.data.WorldRegion,
    onDismiss: () -> Unit,
    onTravel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row {
                Text(
                    text = region.icon,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Column {
                    Text(
                        text = region.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = region.nameCn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = region.description,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "状态",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    RegionStatusBadge(status = region.status)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "章节",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "第${region.chapter}章",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "NPC数量",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "${region.npcIds.size}位",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "任务数量",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "${region.questIds.size}个",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            if (region.status != com.sworddao.phoenix.feature.world.data.RegionStatus.LOCKED) {
                FilledTonalButton(onClick = onTravel) {
                    Text("前往")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
fun TravelConfirmDialog(
    targetRegion: com.sworddao.phoenix.feature.world.data.WorldRegion,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认旅行") },
        text = {
            Column {
                Text(
                    text = "确定要前往 ${targetRegion.name} (${targetRegion.nameCn}) 吗？",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "旅行方式: 步行",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onConfirm) {
                Text("出发")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
