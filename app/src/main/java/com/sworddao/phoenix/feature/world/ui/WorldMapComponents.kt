package com.sworddao.phoenix.feature.world.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.feature.world.data.RegionStatus
import com.sworddao.phoenix.feature.world.data.WorldRegion

@Composable
fun WorldMapCanvas(
    regions: List<WorldRegion>,
    onRegionClick: (WorldRegion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.fillMaxSize(),
    ) {
        // Draw connections between regions
        regions.forEach { region ->
            region.connections.forEach { connectionId ->
                val connectedRegion = regions.find { it.id == connectionId }
                if (connectedRegion != null) {
                    val startColor = when (region.status) {
                        RegionStatus.LOCKED -> Color.Gray.copy(alpha = 0.3f)
                        else -> Color(0xFF8D6E63)
                    }
                    drawLine(
                        color = startColor,
                        start = Offset(
                            region.mapPositionX * size.width,
                            region.mapPositionY * size.height,
                        ),
                        end = Offset(
                            connectedRegion.mapPositionX * size.width,
                            connectedRegion.mapPositionY * size.height,
                        ),
                        strokeWidth = 3f,
                    )
                }
            }
        }

        // Draw region nodes
        regions.forEach { region ->
            val nodeColor = when (region.status) {
                RegionStatus.LOCKED -> Color.Gray.copy(alpha = 0.5f)
                RegionStatus.AVAILABLE -> Color(0xFF4CAF50)
                RegionStatus.CURRENT -> Color(0xFF2196F3)
                RegionStatus.VISITED -> Color(0xFF8BC34A)
                RegionStatus.COMPLETED -> Color(0xFFFFD700)
            }

            val nodeSize = if (region.status == RegionStatus.CURRENT) 40f else 30f

            // Draw node background
            drawCircle(
                color = nodeColor.copy(alpha = 0.3f),
                radius = nodeSize + 10f,
                center = Offset(
                    region.mapPositionX * size.width,
                    region.mapPositionY * size.height,
                ),
            )

            // Draw node
            drawCircle(
                color = nodeColor,
                radius = nodeSize,
                center = Offset(
                    region.mapPositionX * size.width,
                    region.mapPositionY * size.height,
                ),
            )
        }
    }
}

@Composable
fun RegionNode(
    region: WorldRegion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nodeColor = when (region.status) {
        RegionStatus.LOCKED -> Color.Gray.copy(alpha = 0.5f)
        RegionStatus.AVAILABLE -> Color(0xFF4CAF50)
        RegionStatus.CURRENT -> Color(0xFF2196F3)
        RegionStatus.VISITED -> Color(0xFF8BC34A)
        RegionStatus.COMPLETED -> Color(0xFFFFD700)
    }

    Column(
        modifier = modifier.clickable(enabled = region.status != RegionStatus.LOCKED) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(if (region.status == RegionStatus.CURRENT) 56.dp else 44.dp)
                .clip(CircleShape)
                .background(nodeColor)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = region.icon,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = region.name,
            style = MaterialTheme.typography.labelSmall,
            color = when (region.status) {
                RegionStatus.LOCKED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
        )
    }
}

@Composable
fun RegionConnection(
    fromRegion: WorldRegion,
    toRegion: WorldRegion,
    modifier: Modifier = Modifier,
) {
    val isConnected = fromRegion.isUnlocked && toRegion.isUnlocked

    Canvas(
        modifier = modifier.fillMaxWidth().height(2.dp),
    ) {
        drawLine(
            color = if (isConnected) Color(0xFF8D6E63) else Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 3f,
        )
    }
}

@Composable
fun ExplorationProgressCard(
    completionPercentage: Float,
    regionsCompleted: Int,
    totalRegions: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "探索进度",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "完成区域: $regionsCompleted/$totalRegions",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${(completionPercentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun CurrentLocationBanner(
    region: WorldRegion?,
    modifier: Modifier = Modifier,
) {
    if (region != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = region.icon,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "当前位置",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = region.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = region.nameCn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
