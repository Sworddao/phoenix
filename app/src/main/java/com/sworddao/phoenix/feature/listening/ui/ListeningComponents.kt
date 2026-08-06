package com.sworddao.phoenix.feature.listening.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.listening.data.AudioPlaybackState
import com.sworddao.phoenix.feature.listening.data.AudioPlaybackStateInfo
import com.sworddao.phoenix.feature.listening.data.ListeningChoice
import com.sworddao.phoenix.feature.listening.data.ListeningExercise
import com.sworddao.phoenix.feature.listening.data.ListeningSession
import com.sworddao.phoenix.feature.listening.data.ListeningStatistics
import com.sworddao.phoenix.ui.components.BaoCharacter
import com.sworddao.phoenix.ui.components.BaoExpression
import com.sworddao.phoenix.ui.components.rememberReducedMotion

@Composable
fun AudioPlayerCard(
    exercise: ListeningExercise,
    playbackState: AudioPlaybackStateInfo,
    playbackRate: Float,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    onToggleSlow: () -> Unit,
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = exercise.clip.displayText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exercise.clip.text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exercise.clip.english,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            val isPlaying = playbackState.state == AudioPlaybackState.PLAYING
            val reduceMotion = rememberReducedMotion()
            val contentDescription = if (isPlaying) {
                stringResource(R.string.listening_pause)
            } else {
                stringResource(R.string.listening_play)
            }

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .semantics { this.contentDescription = contentDescription },
                contentAlignment = Alignment.Center,
            ) {
                if (isPlaying && !reduceMotion) {
                    val pulse by rememberInfiniteTransition(label = "playback_pulse")
                        .animateFloat(
                            initialValue = 1f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(700, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "playback_pulse_value",
                        )
                    Spacer(
                        modifier = Modifier
                            .size(88.dp)
                            .graphicsLayer {
                                scaleX = pulse
                                scaleY = pulse
                            }
                    )
                }
                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(36.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                OutlinedButton(
                    onClick = onReplay,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.listening_replay),
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedButton(
                    onClick = onToggleSlow,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = if (playbackRate == 0.75f) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.SlowMotionVideo,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (playbackRate == 0.75f) {
                                R.string.listening_slow
                            } else {
                                R.string.listening_normal
                            }
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun ListeningExerciseCard(
    exercise: ListeningExercise,
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
                    imageVector = Icons.Default.VolumeUp,
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
fun ListeningChoiceCard(
    choice: ListeningChoice,
    index: Int,
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
        Text(
            text = choice.text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
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
fun ReplayButton(
    replayCount: Int,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onReplay,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Replay,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.listening_replay),
            fontWeight = FontWeight.Bold,
        )
        if (replayCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "×$replayCount",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun ListeningProgressCard(
    session: ListeningSession?,
    statistics: ListeningStatistics,
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
                text = stringResource(R.string.listening_session_progress),
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
                        stringResource(R.string.listening_xp_earned, session.totalXpEarned),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (statistics.currentStreak > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.listening_streak_label) +
                        " ${statistics.currentStreak} 天",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun BaoListeningHint(
    hintIndex: Int,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReducedMotion()
    val bounce by rememberInfiniteTransition(label = "bao_hint_bounce")
        .animateFloat(
            initialValue = 0f,
            targetValue = if (reduceMotion) 0f else -6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bao_hint_bounce_value",
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
                        0 -> R.string.listening_bao_hint_1
                        1 -> R.string.listening_bao_hint_2
                        2 -> R.string.listening_bao_hint_3
                        3 -> R.string.listening_bao_hint_4
                        else -> R.string.listening_bao_hint_5
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
fun ListeningCompletionDialog(
    session: ListeningSession,
    statistics: ListeningStatistics,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = {
            Text(
                text = stringResource(R.string.listening_complete_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.listening_complete_message),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "✓ ${session.correctAttempts} / ${session.exerciseIds.size} · " +
                        stringResource(R.string.listening_xp_earned, session.totalXpEarned),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (statistics.currentStreak > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.listening_streak_continued,
                            statistics.currentStreak,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(
                    text = stringResource(R.string.listening_complete_continue),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}
