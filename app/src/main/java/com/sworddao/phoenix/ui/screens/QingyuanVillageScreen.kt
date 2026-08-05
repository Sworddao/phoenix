package com.sworddao.phoenix.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sworddao.phoenix.ui.components.BaoCharacter
import com.sworddao.phoenix.ui.components.BaoExpression
import com.sworddao.phoenix.ui.theme.DeepRed
import com.sworddao.phoenix.ui.theme.Gold
import com.sworddao.phoenix.ui.theme.JadeGreen
import com.sworddao.phoenix.ui.theme.JadeGreenDark
import com.sworddao.phoenix.ui.theme.WarmCream

data class VillageLocation(
    val name: String,
    val description: String,
    val emoji: String,
    val positionX: Float,
    val positionY: Float,
    val widthDp: Int,
    val heightDp: Int
)

@Composable
fun QingyuanVillageScreen(
    playerName: String,
    modifier: Modifier = Modifier
) {
    var selectedLocation by remember { mutableStateOf<VillageLocation?>(null) }
    var showBaoMessage by remember { mutableStateOf(false) }
    var baoMessage by remember { mutableStateOf("") }

    val baoMessages = remember {
        listOf(
            "Let's explore together!",
            "I wonder who we'll meet today.",
            "Every conversation is a new adventure.",
            "This village is beautiful, isn't it?",
            "I'm so glad you're here!",
            "Let's take our time and enjoy the scenery.",
            "The mountains look peaceful today.",
            "I can smell something delicious from the bakery!",
            "Shall we visit the tea house?",
            "You're doing great, $playerName!"
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "village_animation")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    val lanternSway by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lantern_sway"
    )

    val waterRipple by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "water_ripple"
    )

    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_offset"
    )

    val treeSway by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tree_sway"
    )

    val locations = remember {
        listOf(
            VillageLocation(
                name = "Grandma Mei's Bakery",
                description = "The warm smell of fresh bread fills the air. Grandma Mei is famous for her mooncakes and steamed buns. She'll teach you food vocabulary and how to order treats.",
                emoji = "🥟",
                positionX = 0.12f,
                positionY = 0.48f,
                widthDp = 90,
                heightDp = 60
            ),
            VillageLocation(
                name = "Restaurant",
                description = "Owner Lin's restaurant serves the best hot pot in the village. Learn to order food, ask about ingredients, and understand menus.",
                emoji = "🍜",
                positionX = 0.52f,
                positionY = 0.46f,
                widthDp = 100,
                heightDp = 65
            ),
            VillageLocation(
                name = "Tea House",
                description = "A peaceful place to practice conversation. The tea master will teach you about Chinese tea culture and help you learn polite expressions.",
                emoji = "🍵",
                positionX = 0.33f,
                positionY = 0.38f,
                widthDp = 85,
                heightDp = 55
            ),
            VillageLocation(
                name = "Village Square",
                description = "The heart of the village where neighbors gather. A great place to meet new people and practice everyday greetings.",
                emoji = "🏘️",
                positionX = 0.4f,
                positionY = 0.6f,
                widthDp = 80,
                heightDp = 50
            ),
            VillageLocation(
                name = "Village Exit",
                description = "The path leads deeper into China. Once you've made friends here, new adventures await beyond the mountains.",
                emoji = "🚶",
                positionX = 0.82f,
                positionY = 0.55f,
                widthDp = 60,
                heightDp = 40
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .semantics {
                contentDescription = "Qingyuan Village - Your adventure begins here. Tap on locations to explore."
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                drawVillageScene(
                    floatOffset = floatOffset,
                    lanternSway = lanternSway,
                    waterRipple = waterRipple,
                    cloudOffset = cloudOffset,
                    treeSway = treeSway
                )
            }

            locations.forEach { location ->
                LocationButton(
                    location = location,
                    onClick = { selectedLocation = location }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .clickable {
                        baoMessage = baoMessages.random()
                        showBaoMessage = true
                    }
                    .semantics {
                        contentDescription = "Bao the red panda. Tap to hear encouragement."
                    }
            ) {
                BaoCharacter(
                    size = 80.dp,
                    expression = BaoExpression.HAPPY
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to Qingyuan Village",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Tap on buildings to explore",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Explore the village to meet new friends, $playerName!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    selectedLocation?.let { location ->
        LocationDialog(
            location = location,
            onDismiss = { selectedLocation = null }
        )
    }

    if (showBaoMessage) {
        BaoMessageDialog(
            message = baoMessage,
            onDismiss = { showBaoMessage = false }
        )
    }
}

@Composable
private fun LocationButton(
    location: VillageLocation,
    onClick: () -> Unit
) {
    val density = LocalDensity.current

    val offsetX = with(density) { (location.positionX * 1f).toDp() }
    val offsetY = with(density) { (location.positionY * 500f).toDp() }
    val buttonWidth = with(density) { location.widthDp.dp }
    val buttonHeight = with(density) { location.heightDp.dp }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.toPx().toInt(), offsetY.toPx().toInt()) }
            .size(width = buttonWidth, height = buttonHeight)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${location.emoji} ${location.name}. Tap to learn more."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = location.emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = location.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun LocationDialog(
    location: VillageLocation,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = location.emoji,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = location.description,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Got it!",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun BaoMessageDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BaoCharacter(
                    size = 48.dp,
                    expression = BaoExpression.HAPPY
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Bao says:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Thanks, Bao!",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

private fun DrawScope.drawVillageScene(
    floatOffset: Float,
    lanternSway: Float,
    waterRipple: Float,
    cloudOffset: Float,
    treeSway: Float
) {
    val width = size.width
    val height = size.height

    // Sky gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF87CEEB),
                Color(0xFFB3E5FC),
                Color(0xFFE0F7FA)
            )
        )
    )

    // Clouds
    drawCloud(
        center = Offset(width * 0.2f + cloudOffset, height * 0.1f),
        scale = 1f
    )
    drawCloud(
        center = Offset(width * 0.7f - cloudOffset * 0.5f, height * 0.08f),
        scale = 0.7f
    )
    drawCloud(
        center = Offset(width * 0.5f + cloudOffset * 0.3f, height * 0.15f),
        scale = 0.5f
    )

    // Sun
    drawCircle(
        color = Gold,
        radius = 45f,
        center = Offset(width * 0.85f, height * 0.12f + floatOffset)
    )

    // Sun rays
    for (i in 0..7) {
        val angle = Math.toRadians((i * 45).toDouble())
        val rayLength = 25f
        drawLine(
            color = Gold,
            start = Offset(
                x = width * 0.85f + (Math.cos(angle) * 55).toFloat(),
                y = height * 0.12f + floatOffset + (Math.sin(angle) * 55).toFloat()
            ),
            end = Offset(
                x = width * 0.85f + (Math.cos(angle) * (55 + rayLength)).toFloat(),
                y = height * 0.12f + floatOffset + (Math.sin(angle) * (55 + rayLength)).toFloat()
            ),
            strokeWidth = 3f
        )
    }

    // Mountains (background)
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height * 0.6f)
            lineTo(width * 0.15f, height * 0.35f)
            lineTo(width * 0.3f, height * 0.45f)
            lineTo(width * 0.5f, height * 0.3f)
            lineTo(width * 0.7f, height * 0.4f)
            lineTo(width * 0.85f, height * 0.25f)
            lineTo(width, height * 0.5f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        },
        color = Color(0xFF4A7C59).copy(alpha = 0.6f)
    )

    // Middle hills
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height * 0.7f)
            quadraticBezierTo(
                width * 0.2f, height * 0.55f,
                width * 0.4f, height * 0.65f
            )
            quadraticBezierTo(
                width * 0.6f, height * 0.75f,
                width * 0.8f, height * 0.6f
            )
            quadraticBezierTo(
                width * 0.9f, height * 0.55f,
                width, height * 0.65f
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        },
        color = JadeGreen.copy(alpha = 0.8f)
    )

    // River
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF4FC3F7),
                Color(0xFF29B6F6),
                Color(0xFF039BE5)
            )
        ),
        topLeft = Offset(width * 0.1f, height * 0.78f),
        size = Size(width * 0.8f, height * 0.08f)
    )

    // Water ripples
    for (i in 0..4) {
        val rippleX = width * (0.15f + i * 0.18f)
        val rippleY = height * 0.82f + (waterRipple * 3f)
        drawOval(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(rippleX - 15f, rippleY - 2f),
            size = Size(30f, 4f)
        )
    }

    // Bridge
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(width * 0.35f, height * 0.76f)
            quadraticBezierTo(
                width * 0.5f, height * 0.72f,
                width * 0.65f, height * 0.76f
            )
            lineTo(width * 0.65f, height * 0.78f)
            quadraticBezierTo(
                width * 0.5f, height * 0.74f,
                width * 0.35f, height * 0.78f
            )
            close()
        },
        color = Color(0xFF8D6E63)
    )

    // Bridge railings
    drawLine(
        color = Color(0xFF6D4C41),
        start = Offset(width * 0.38f, height * 0.76f),
        end = Offset(width * 0.38f, height * 0.73f),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF6D4C41),
        start = Offset(width * 0.5f, height * 0.74f),
        end = Offset(width * 0.5f, height * 0.71f),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF6D4C41),
        start = Offset(width * 0.62f, height * 0.76f),
        end = Offset(width * 0.62f, height * 0.73f),
        strokeWidth = 3f
    )

    // Stone pathway
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(width * 0.5f, height * 0.95f)
            quadraticBezierTo(
                width * 0.45f, height * 0.85f,
                width * 0.5f, height * 0.78f
            )
            quadraticBezierTo(
                width * 0.55f, height * 0.85f,
                width * 0.5f, height * 0.95f
            )
        },
        color = Color(0xFFBDBDBD)
    )

    // Stone details on path
    for (i in 0..3) {
        val stoneY = height * (0.82f + i * 0.03f)
        drawOval(
            color = Color(0xFF9E9E9E),
            topLeft = Offset(width * 0.48f, stoneY),
            size = Size(8f, 4f)
        )
    }

    // Foreground ground
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height * 0.85f)
            quadraticBezierTo(
                width * 0.25f, height * 0.8f,
                width * 0.5f, height * 0.85f
            )
            quadraticBezierTo(
                width * 0.75f, height * 0.9f,
                width, height * 0.82f
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        },
        color = Color(0xFF7CB342)
    )

    // Bamboo (left side)
    drawBamboo(
        baseX = width * 0.05f,
        baseY = height * 0.7f,
        bambooHeight = height * 0.4f,
        sway = treeSway
    )
    drawBamboo(
        baseX = width * 0.1f,
        baseY = height * 0.72f,
        bambooHeight = height * 0.35f,
        sway = treeSway * 0.8f
    )

    // Bamboo (right side)
    drawBamboo(
        baseX = width * 0.92f,
        baseY = height * 0.68f,
        bambooHeight = height * 0.42f,
        sway = -treeSway
    )
    drawBamboo(
        baseX = width * 0.88f,
        baseY = height * 0.7f,
        bambooHeight = height * 0.38f,
        sway = -treeSway * 0.9f
    )

    // Trees
    drawTree(
        baseX = width * 0.15f,
        baseY = height * 0.72f,
        trunkHeight = 40f,
        foliageRadius = 30f,
        sway = treeSway
    )
    drawTree(
        baseX = width * 0.82f,
        baseY = height * 0.7f,
        trunkHeight = 45f,
        foliageRadius = 35f,
        sway = -treeSway
    )

    // Bakery building
    drawBuilding(
        x = width * 0.12f,
        y = height * 0.5f,
        buildingWidth = 90f,
        buildingHeight = 60f,
        roofColor = DeepRed,
        wallColor = WarmCream
    )

    // Restaurant building
    drawBuilding(
        x = width * 0.52f,
        y = height * 0.48f,
        buildingWidth = 100f,
        buildingHeight = 65f,
        roofColor = Color(0xFF8B4513),
        wallColor = WarmCream
    )

    // Tea House
    drawBuilding(
        x = width * 0.33f,
        y = height * 0.4f,
        buildingWidth = 85f,
        buildingHeight = 55f,
        roofColor = JadeGreenDark,
        wallColor = WarmCream
    )

    // Village Square (open area with stones)
    drawRoundRect(
        color = Color(0xFFD7CCC8),
        topLeft = Offset(width * 0.4f, height * 0.62f),
        size = Size(80f, 50f),
        cornerRadius = CornerRadius(8f)
    )

    // Lanterns
    drawLantern(
        x = width * 0.2f,
        y = height * 0.48f + lanternSway,
        lanternSize = 15f
    )
    drawLantern(
        x = width * 0.6f,
        y = height * 0.46f - lanternSway,
        lanternSize = 15f
    )
    drawLantern(
        x = width * 0.4f,
        y = height * 0.38f + lanternSway * 0.5f,
        lanternSize = 12f
    )

    // Village Exit sign
    drawRoundRect(
        color = Color(0xFF8D6E63),
        topLeft = Offset(width * 0.82f, height * 0.55f),
        size = Size(60f, 40f),
        cornerRadius = CornerRadius(4f)
    )
    drawRoundRect(
        color = WarmCream,
        topLeft = Offset(width * 0.83f, height * 0.56f),
        size = Size(56f, 36f),
        cornerRadius = CornerRadius(2f)
    )
}

private fun DrawScope.drawCloud(center: Offset, scale: Float) {
    val cloudColor = Color.White.copy(alpha = 0.9f)
    drawCircle(
        color = cloudColor,
        radius = 20f * scale,
        center = center
    )
    drawCircle(
        color = cloudColor,
        radius = 15f * scale,
        center = Offset(center.x - 15f * scale, center.y + 5f * scale)
    )
    drawCircle(
        color = cloudColor,
        radius = 18f * scale,
        center = Offset(center.x + 15f * scale, center.y + 3f * scale)
    )
}

private fun DrawScope.drawBamboo(baseX: Float, baseY: Float, bambooHeight: Float, sway: Float) {
    // Bamboo stalk
    drawLine(
        color = Color(0xFF689F38),
        start = Offset(baseX, baseY),
        end = Offset(baseX + sway, baseY - bambooHeight),
        strokeWidth = 6f
    )

    // Bamboo segments
    for (i in 1..4) {
        val segY = baseY - bambooHeight * (i * 0.2f)
        drawLine(
            color = Color(0xFF558B2F),
            start = Offset(baseX + sway * (i * 0.2f) - 4f, segY),
            end = Offset(baseX + sway * (i * 0.2f) + 4f, segY),
            strokeWidth = 2f
        )
    }

    // Bamboo leaves
    for (i in 0..2) {
        val leafY = baseY - bambooHeight * (0.6f + i * 0.15f)
        val leafX = baseX + sway * 0.8f
        drawOval(
            color = Color(0xFF7CB342),
            topLeft = Offset(leafX + (i * 8f) - 5f, leafY - 3f),
            size = Size(12f, 6f)
        )
    }
}

private fun DrawScope.drawTree(baseX: Float, baseY: Float, trunkHeight: Float, foliageRadius: Float, sway: Float) {
    // Trunk
    drawRect(
        color = Color(0xFF8D6E63),
        topLeft = Offset(baseX - 5f, baseY - trunkHeight),
        size = Size(10f, trunkHeight)
    )

    // Foliage
    drawCircle(
        color = JadeGreen,
        radius = foliageRadius,
        center = Offset(baseX + sway, baseY - trunkHeight - foliageRadius * 0.5f)
    )
    drawCircle(
        color = JadeGreen.copy(alpha = 0.8f),
        radius = foliageRadius * 0.7f,
        center = Offset(baseX + sway + foliageRadius * 0.3f, baseY - trunkHeight - foliageRadius * 0.3f)
    )
}

private fun DrawScope.drawBuilding(x: Float, y: Float, buildingWidth: Float, buildingHeight: Float, roofColor: Color, wallColor: Color) {
    // Wall
    drawRect(
        color = wallColor,
        topLeft = Offset(x, y),
        size = Size(buildingWidth, buildingHeight)
    )

    // Roof
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(x - 8f, y)
            lineTo(x + buildingWidth / 2, y - 30f)
            lineTo(x + buildingWidth + 8f, y)
            close()
        },
        color = roofColor
    )

    // Door
    drawRoundRect(
        color = Color(0xFF5D4037),
        topLeft = Offset(x + buildingWidth / 2 - 10f, y + buildingHeight - 35f),
        size = Size(20f, 35f),
        cornerRadius = CornerRadius(3f)
    )

    // Window
    drawRoundRect(
        color = Color(0xFF81D4FA),
        topLeft = Offset(x + 10f, y + 15f),
        size = Size(18f, 18f),
        cornerRadius = CornerRadius(2f)
    )
    drawLine(
        color = Color(0xFF5D4037),
        start = Offset(x + 19f, y + 15f),
        end = Offset(x + 19f, y + 33f),
        strokeWidth = 2f
    )
    drawLine(
        color = Color(0xFF5D4037),
        start = Offset(x + 10f, y + 24f),
        end = Offset(x + 28f, y + 24f),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawLantern(x: Float, y: Float, lanternSize: Float) {
    // Lantern string
    drawLine(
        color = Color(0xFF8D6E63),
        start = Offset(x, y - 20f),
        end = Offset(x, y),
        strokeWidth = 1f
    )

    // Lantern body
    drawOval(
        color = DeepRed,
        topLeft = Offset(x - lanternSize / 2, y),
        size = Size(lanternSize, lanternSize * 1.3f)
    )

    // Lantern top
    drawRect(
        color = Gold,
        topLeft = Offset(x - lanternSize / 2 - 2f, y - 3f),
        size = Size(lanternSize + 4f, 5f)
    )

    // Lantern bottom
    drawRect(
        color = Gold,
        topLeft = Offset(x - lanternSize / 2 - 2f, y + lanternSize * 1.3f - 2f),
        size = Size(lanternSize + 4f, 5f)
    )

    // Lantern glow
    drawCircle(
        color = Gold.copy(alpha = 0.2f),
        radius = lanternSize * 0.8f,
        center = Offset(x, y + lanternSize * 0.65f)
    )
}
