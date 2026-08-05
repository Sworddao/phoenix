package com.sworddao.phoenix.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sworddao.phoenix.ui.theme.JadeGreen
import com.sworddao.phoenix.ui.theme.WarmCream
import com.sworddao.phoenix.ui.theme.Gold

@Composable
fun QingyuanVillageScreen(
    playerName: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "village_animation")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Qingyuan Village",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Village illustration placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Animated village scene
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                val width = size.width
                val height = size.height

                // Sky gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF87CEEB),
                            Color(0xFFE0F7FA)
                        )
                    )
                )

                // Mountains
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, height * 0.6f)
                        lineTo(width * 0.3f, height * 0.3f)
                        lineTo(width * 0.5f, height * 0.5f)
                        lineTo(width * 0.7f, height * 0.25f)
                        lineTo(width, height * 0.55f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    },
                    color = JadeGreen.copy(alpha = 0.6f)
                )

                // Foreground hills
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, height * 0.75f)
                        quadraticBezierTo(
                            width * 0.25f, height * 0.65f,
                            width * 0.5f, height * 0.75f
                        )
                        quadraticBezierTo(
                            width * 0.75f, height * 0.85f,
                            width, height * 0.7f
                        )
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    },
                    color = JadeGreen
                )

                // Sun
                drawCircle(
                    color = Gold,
                    radius = 40f,
                    center = Offset(width * 0.8f, height * 0.15f + floatOffset)
                )

                // Sun rays
                for (i in 0..7) {
                    val angle = Math.toRadians((i * 45).toDouble())
                    val rayLength = 20f
                    drawLine(
                        color = Gold,
                        start = Offset(
                            x = width * 0.8f + (Math.cos(angle) * 50).toFloat(),
                            y = height * 0.15f + floatOffset + (Math.sin(angle) * 50).toFloat()
                        ),
                        end = Offset(
                            x = width * 0.8f + (Math.cos(angle) * (50 + rayLength)).toFloat(),
                            y = height * 0.15f + floatOffset + (Math.sin(angle) * (50 + rayLength)).toFloat()
                        ),
                        strokeWidth = 3f
                    )
                }

                // Small houses
                val houseWidth = 60f
                val houseHeight = 40f

                // House 1
                drawRect(
                    color = WarmCream,
                    topLeft = Offset(width * 0.2f, height * 0.7f - houseHeight),
                    size = androidx.compose.ui.geometry.Size(houseWidth, houseHeight)
                )
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(width * 0.2f - 5f, height * 0.7f - houseHeight)
                        lineTo(width * 0.2f + houseWidth / 2, height * 0.7f - houseHeight - 30f)
                        lineTo(width * 0.2f + houseWidth + 5f, height * 0.7f - houseHeight)
                        close()
                    },
                    color = Color(0xFF8B4513)
                )

                // House 2
                drawRect(
                    color = WarmCream,
                    topLeft = Offset(width * 0.5f, height * 0.72f - houseHeight),
                    size = androidx.compose.ui.geometry.Size(houseWidth, houseHeight)
                )
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(width * 0.5f - 5f, height * 0.72f - houseHeight)
                        lineTo(width * 0.5f + houseWidth / 2, height * 0.72f - houseHeight - 30f)
                        lineTo(width * 0.5f + houseWidth + 5f, height * 0.72f - houseHeight)
                        close()
                    },
                    color = Color(0xFF8B4513)
                )

                // Trees
                for (i in 0..2) {
                    val treeX = width * (0.1f + i * 0.35f)
                    val treeY = height * 0.68f

                    // Trunk
                    drawRect(
                        color = Color(0xFF8B4513),
                        topLeft = Offset(treeX - 5f, treeY),
                        size = androidx.compose.ui.geometry.Size(10f, 30f)
                    )

                    // Foliage
                    drawCircle(
                        color = JadeGreen,
                        radius = 25f,
                        center = Offset(treeX, treeY - 10f)
                    )
                }
            }

            // Village name overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Qingyuan Village",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your adventure begins here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Bottom info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Welcome to Phoenix, $playerName!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
