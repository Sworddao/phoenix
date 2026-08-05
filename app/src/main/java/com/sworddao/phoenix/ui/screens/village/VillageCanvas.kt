package com.sworddao.phoenix.ui.screens.village

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.sworddao.phoenix.ui.theme.DeepRed
import com.sworddao.phoenix.ui.theme.Gold
import com.sworddao.phoenix.ui.theme.JadeGreen
import com.sworddao.phoenix.ui.theme.JadeGreenDark
import com.sworddao.phoenix.ui.theme.WarmCream

fun DrawScope.drawVillageScene(
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
