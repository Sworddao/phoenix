package com.sworddao.phoenix.feature.reading.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.reading.data.CharacterRevealState
import com.sworddao.phoenix.feature.reading.data.HanziRenderer
import com.sworddao.phoenix.feature.reading.data.ReadingChoice
import com.sworddao.phoenix.feature.reading.data.ReadingExercise
import com.sworddao.phoenix.feature.reading.data.ReadingSession
import com.sworddao.phoenix.feature.reading.data.ReadingStatistics
import com.sworddao.phoenix.feature.reading.data.RenderedHanziSpan
import com.sworddao.phoenix.ui.components.BaoCharacter
import com.sworddao.phoenix.ui.components.BaoExpression
import com.sworddao.phoenix.ui.components.rememberReducedMotion

@Composable
fun HanziDisplayCard(
    exercise: ReadingExercise,
    renderer: HanziRenderer,
    revealMode: CharacterRevealState,
    isHanziRevealed: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReducedMotion()
    val targetMode = when {
        isHanziRevealed && revealMode == CharacterRevealState.TONE_COLORED_PINYIN -> revealMode
        isHanziRevealed && revealMode == CharacterRevealState.HANZI_ONLY -> revealMode
        isHanziRevealed -> CharacterRevealState.HANZI_AND_PINYIN
        else -> revealMode
    }
    val rendered = renderer.render(exercise.hanzi, exercise.pinyin, targetMode)
    val tapToRevealEnabled =
        revealMode == CharacterRevealState.TAP_TO_REVEAL ||
            revealMode == CharacterRevealState.PINYIN_ONLY

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val displayText = if (isHanziRevealed) rendered.hanzi ?: "" else rendered.maskedHanzi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isHanziRevealed && tapToRevealEnabled) { onReveal() }
                    .semantics {
                        contentDescription = exercise.hanzi
                        if (!isHanziRevealed && tapToRevealEnabled) {
                            onClick(label = "reveal hanzi") { onReveal(); true }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayText,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = if (isHanziRevealed) 12.sp else 4.sp,
                    color = if (isHanziRevealed) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                )
            }

            if (!isHanziRevealed && tapToRevealEnabled && revealMode == CharacterRevealState.TAP_TO_REVEAL) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.reading_tap_to_reveal),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (rendered.pinyin.isNotEmpty()) {
                ToneColoredPinyin(rendered.toneColoredPinyin)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = exercise.english,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ToneColoredPinyin(
    hua: List<RenderedHanziSpan>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        hua.forEach { span ->
            val spanColor = span.toneColor?.color?.let { Color(it) }
            Text(
                text = span.text,
                style = MaterialTheme.typography.headlineSmall,
                color = spanColor ?: MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
fun ReadingExerciseCard(
    exercise: ReadingExercise,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = exercise.prompt,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (exercise.context.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exercise.context,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
fun ReadingChoiceCard(
    choice: ReadingChoice,
    isSelected: Boolean,
    isRevealed: Boolean,
    wasCorrectChoice: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when {
        !isRevealed -> MaterialTheme.colorScheme.surface
        wasCorrectChoice -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = containerColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = choice.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (choice.hanzi != null && choice.hanzi.isNotEmpty() && choice.hanzi != choice.text) {
                Text(
                    text = choice.hanzi,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (isRevealed && (wasCorrectChoice || isSelected)) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (wasCorrectChoice) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (wasCorrectChoice) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}

@Composable
fun ReadingProgressCard(
    session: ReadingSession?,
    statistics: ReadingStatistics,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(
                text = stringResource(R.string.reading_session_progress),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (session != null && session.exerciseIds.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { session.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${session.currentExerciseIndex + 1} / ${session.exerciseIds.size} · " +
                        stringResource(R.string.reading_xp_earned, session.totalXpEarned),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (statistics.currentStreak > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.reading_streak_label) +
                        " ${statistics.currentStreak} 天",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun BaoReadingHint(
    hintIndex: Int,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReducedMotion()
    val bounce by rememberInfiniteTransition(label = "bao_reading_bounce")
        .animateFloat(
            initialValue = 0f,
            targetValue = if (reduceMotion) 0f else -6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bao_reading_bounce_value",
        )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    translationY = bounce
                }
        ) {
            BaoCharacter(
                modifier = Modifier.size(56.dp),
                expression = BaoExpression.HAPPY,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
        ) {
            Text(
                text = stringResource(
                    when (hintIndex % 5) {
                        0 -> R.string.reading_bao_hint_1
                        1 -> R.string.reading_bao_hint_2
                        2 -> R.string.reading_bao_hint_3
                        3 -> R.string.reading_bao_hint_4
                        else -> R.string.reading_bao_hint_5
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
fun RevealButton(
    isHanziRevealed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !isHanziRevealed,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(
                if (isHanziRevealed) R.string.reading_revealed else R.string.reading_reveal
            ),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ReadingCompletionDialog(
    session: ReadingSession,
    statistics: ReadingStatistics,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = {
            Text(
                text = stringResource(R.string.reading_complete_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.reading_complete_message),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "✓ ${session.correctAttempts} / ${session.exerciseIds.size} · " +
                        stringResource(R.string.reading_xp_earned, session.totalXpEarned) +
                        " · ${session.totalReveals} ${stringResource(R.string.reading_total_reveals)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (statistics.currentStreak > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.reading_streak_continued,
                            statistics.currentStreak,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(
                    text = stringResource(R.string.reading_complete_continue),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}