package com.sworddao.phoenix.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.ui.theme.Gold
import com.sworddao.phoenix.ui.theme.JadeGreen
import com.sworddao.phoenix.ui.theme.WarmCream

@Composable
fun PhoenixLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_floating")

    val floatingOffset by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "floating"
        )
    } else {
        androidx.compose.animation.core.animateFloatAsState(
            targetValue = 0f,
            label = "static_floating"
        )
    }

    Canvas(modifier = modifier.size(size)) {
        val centerX = size.toPx() / 2
        val centerY = size.toPx() / 2 + floatingOffset
        val radius = size.toPx() * 0.35f

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Gold.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = radius * 1.5f
            ),
            radius = radius * 1.5f,
            center = Offset(centerX, centerY)
        )

        // Phoenix body - stylized flame/bird shape
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Gold,
                    JadeGreen
                ),
                center = Offset(centerX, centerY),
                radius = radius
            ),
            radius = radius,
            center = Offset(centerX, centerY)
        )

        // Inner circle
        drawCircle(
            color = WarmCream,
            radius = radius * 0.6f,
            center = Offset(centerX, centerY)
        )

        // Phoenix symbol - simplified flame/wing
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    JadeGreen,
                    Gold.copy(alpha = 0.8f)
                ),
                center = Offset(centerX, centerY),
                radius = radius * 0.45f
            ),
            radius = radius * 0.45f,
            center = Offset(centerX, centerY)
        )

        // Center dot
        drawCircle(
            color = WarmCream,
            radius = radius * 0.15f,
            center = Offset(centerX, centerY)
        )

        // Wing accents
        drawCircle(
            color = Gold.copy(alpha = 0.6f),
            radius = radius * 0.08f,
            center = Offset(centerX - radius * 0.3f, centerY - radius * 0.2f)
        )

        drawCircle(
            color = Gold.copy(alpha = 0.6f),
            radius = radius * 0.08f,
            center = Offset(centerX + radius * 0.3f, centerY - radius * 0.2f)
        )
    }
}
