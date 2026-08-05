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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.ui.theme.DeepRed
import com.sworddao.phoenix.ui.theme.Gold
import com.sworddao.phoenix.ui.theme.WarmCream

enum class BaoExpression {
    HAPPY,
    EXCITED,
    WAVE,
    THINK,
    SLEEP
}

@Composable
fun BaoCharacter(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    expression: BaoExpression = BaoExpression.HAPPY
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bao_animation")

    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val earWiggle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ear_wiggle"
    )

    Canvas(modifier = modifier.size(size)) {
        val centerX = size.toPx() / 2
        val centerY = size.toPx() / 2 + bounceOffset
        val bodyRadius = size.toPx() * 0.3f

        // Shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.1f),
            topLeft = Offset(
                centerX - bodyRadius * 0.8f,
                centerY + bodyRadius * 0.8f
            ),
            size = Size(
                bodyRadius * 1.6f,
                bodyRadius * 0.3f
            )
        )

        // Body (red panda color)
        drawCircle(
            color = DeepRed,
            radius = bodyRadius,
            center = Offset(centerX, centerY)
        )

        // Belly (lighter)
        drawCircle(
            color = WarmCream,
            radius = bodyRadius * 0.65f,
            center = Offset(centerX, centerY + bodyRadius * 0.1f)
        )

        // Left ear
        drawCircle(
            color = DeepRed,
            radius = bodyRadius * 0.25f,
            center = Offset(
                centerX - bodyRadius * 0.5f,
                centerY - bodyRadius * 0.7f + earWiggle
            )
        )
        drawCircle(
            color = WarmCream,
            radius = bodyRadius * 0.15f,
            center = Offset(
                centerX - bodyRadius * 0.5f,
                centerY - bodyRadius * 0.7f + earWiggle
            )
        )

        // Right ear
        drawCircle(
            color = DeepRed,
            radius = bodyRadius * 0.25f,
            center = Offset(
                centerX + bodyRadius * 0.5f,
                centerY - bodyRadius * 0.7f - earWiggle
            )
        )
        drawCircle(
            color = WarmCream,
            radius = bodyRadius * 0.15f,
            center = Offset(
                centerX + bodyRadius * 0.5f,
                centerY - bodyRadius * 0.7f - earWiggle
            )
        )

        // Face (white patches)
        drawCircle(
            color = WarmCream,
            radius = bodyRadius * 0.55f,
            center = Offset(centerX, centerY - bodyRadius * 0.1f)
        )

        // Eyes
        val eyeOffset = bodyRadius * 0.18f
        val eyeRadius = bodyRadius * 0.08f

        when (expression) {
            BaoExpression.HAPPY -> {
                // Normal eyes
                drawCircle(
                    color = Color.Black,
                    radius = eyeRadius,
                    center = Offset(centerX - eyeOffset, centerY - bodyRadius * 0.15f)
                )
                drawCircle(
                    color = Color.Black,
                    radius = eyeRadius,
                    center = Offset(centerX + eyeOffset, centerY - bodyRadius * 0.15f)
                )
                // Eye shine
                drawCircle(
                    color = Color.White,
                    radius = eyeRadius * 0.4f,
                    center = Offset(centerX - eyeOffset + eyeRadius * 0.3f, centerY - bodyRadius * 0.18f)
                )
                drawCircle(
                    color = Color.White,
                    radius = eyeRadius * 0.4f,
                    center = Offset(centerX + eyeOffset + eyeRadius * 0.3f, centerY - bodyRadius * 0.18f)
                )
            }
            BaoExpression.EXCITED -> {
                // Star eyes
                drawCircle(
                    color = Gold,
                    radius = eyeRadius * 1.3f,
                    center = Offset(centerX - eyeOffset, centerY - bodyRadius * 0.15f)
                )
                drawCircle(
                    color = Gold,
                    radius = eyeRadius * 1.3f,
                    center = Offset(centerX + eyeOffset, centerY - bodyRadius * 0.15f)
                )
            }
            BaoExpression.WAVE -> {
                // Winking
                drawCircle(
                    color = Color.Black,
                    radius = eyeRadius,
                    center = Offset(centerX - eyeOffset, centerY - bodyRadius * 0.15f)
                )
                // Wink line
                drawLine(
                    color = Color.Black,
                    start = Offset(centerX + eyeOffset - eyeRadius, centerY - bodyRadius * 0.15f),
                    end = Offset(centerX + eyeOffset + eyeRadius, centerY - bodyRadius * 0.15f),
                    strokeWidth = eyeRadius * 0.5f
                )
            }
            BaoExpression.THINK -> {
                // Looking up
                drawCircle(
                    color = Color.Black,
                    radius = eyeRadius,
                    center = Offset(centerX - eyeOffset, centerY - bodyRadius * 0.2f)
                )
                drawCircle(
                    color = Color.Black,
                    radius = eyeRadius,
                    center = Offset(centerX + eyeOffset, centerY - bodyRadius * 0.2f)
                )
                // Thought bubble
                drawCircle(
                    color = Color.White,
                    radius = bodyRadius * 0.08f,
                    center = Offset(centerX + bodyRadius * 0.6f, centerY - bodyRadius * 0.5f)
                )
                drawCircle(
                    color = Color.White,
                    radius = bodyRadius * 0.05f,
                    center = Offset(centerX + bodyRadius * 0.45f, centerY - bodyRadius * 0.35f)
                )
            }
            BaoExpression.SLEEP -> {
                // Closed eyes (lines)
                drawLine(
                    color = Color.Black,
                    start = Offset(centerX - eyeOffset - eyeRadius, centerY - bodyRadius * 0.15f),
                    end = Offset(centerX - eyeOffset + eyeRadius, centerY - bodyRadius * 0.15f),
                    strokeWidth = eyeRadius * 0.5f
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(centerX + eyeOffset - eyeRadius, centerY - bodyRadius * 0.15f),
                    end = Offset(centerX + eyeOffset + eyeRadius, centerY - bodyRadius * 0.15f),
                    strokeWidth = eyeRadius * 0.5f
                )
                // Zzz
                drawCircle(
                    color = Color.LightGray,
                    radius = bodyRadius * 0.06f,
                    center = Offset(centerX + bodyRadius * 0.5f, centerY - bodyRadius * 0.6f)
                )
                drawCircle(
                    color = Color.LightGray,
                    radius = bodyRadius * 0.04f,
                    center = Offset(centerX + bodyRadius * 0.6f, centerY - bodyRadius * 0.75f)
                )
            }
        }

        // Nose
        drawOval(
            color = Color.Black,
            topLeft = Offset(
                centerX - bodyRadius * 0.06f,
                centerY + bodyRadius * 0.0f
            ),
            size = Size(
                bodyRadius * 0.12f,
                bodyRadius * 0.08f
            )
        )

        // Mouth (smile)
        drawLine(
            color = Color.Black,
            start = Offset(centerX - bodyRadius * 0.1f, centerY + bodyRadius * 0.1f),
            end = Offset(centerX, centerY + bodyRadius * 0.15f),
            strokeWidth = bodyRadius * 0.03f
        )
        drawLine(
            color = Color.Black,
            start = Offset(centerX, centerY + bodyRadius * 0.15f),
            end = Offset(centerX + bodyRadius * 0.1f, centerY + bodyRadius * 0.1f),
            strokeWidth = bodyRadius * 0.03f
        )

        // Arms
        val armColor = DeepRed
        // Left arm
        drawCircle(
            color = armColor,
            radius = bodyRadius * 0.15f,
            center = Offset(
                centerX - bodyRadius * 0.75f,
                centerY + bodyRadius * 0.1f
            )
        )
        // Right arm
        drawCircle(
            color = armColor,
            radius = bodyRadius * 0.15f,
            center = Offset(
                centerX + bodyRadius * 0.75f,
                centerY + bodyRadius * 0.1f
            )
        )

        // Tail (fluffy red panda tail)
        drawCircle(
            color = DeepRed,
            radius = bodyRadius * 0.3f,
            center = Offset(
                centerX + bodyRadius * 0.5f,
                centerY + bodyRadius * 0.5f
            )
        )
        drawCircle(
            color = WarmCream,
            radius = bodyRadius * 0.15f,
            center = Offset(
                centerX + bodyRadius * 0.5f,
                centerY + bodyRadius * 0.5f
            )
        )
    }
}
