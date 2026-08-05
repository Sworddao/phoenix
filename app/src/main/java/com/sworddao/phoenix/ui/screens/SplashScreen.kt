package com.sworddao.phoenix.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sworddao.phoenix.R
import com.sworddao.phoenix.ui.components.PhoenixLogo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = android.provider.Settings.Global.getFloat(
        androidx.compose.ui.platform.LocalContext.current.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) == 0f

    val backgroundAlpha = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.95f) }
    val titleAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            backgroundAlpha.snapTo(1f)
            logoAlpha.snapTo(1f)
            logoScale.snapTo(1f)
            titleAlpha.snapTo(1f)
            taglineAlpha.snapTo(1f)
            showContent = true
            delay(2000)
            onNavigateToWelcome()
        } else {
            backgroundAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing)
            )
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = LinearEasing)
            )
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = LinearEasing)
            )
            showContent = true
            delay(200)
            titleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = LinearEasing)
            )
            delay(200)
            taglineAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = LinearEasing)
            )
            delay(1500)
            backgroundAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing)
            )
            onNavigateToWelcome()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(backgroundAlpha.value)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhoenixLogo(
                size = 140.dp,
                animated = true,
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(durationMillis = 600, easing = LinearEasing)),
                exit = fadeOut()
            ) {
                Text(
                    text = stringResource(R.string.splash_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(titleAlpha.value)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(durationMillis = 600, easing = LinearEasing)),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(taglineAlpha.value)
                ) {
                    Text(
                        text = stringResource(R.string.splash_tagline_first),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = stringResource(R.string.splash_tagline_second),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
