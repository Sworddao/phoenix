package com.sworddao.phoenix.feature.writing.ui

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
import androidx.compose.material.icons.filled.Create
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.writing.data.EngineStrokeFeedback
import com.sworddao.phoenix.feature.writing.data.StrokeDirection
import com.sworddao.phoenix.feature.writing.data.WritingExercise
import com.sworddao.phoenix.feature.writing.data.WritingResult
import com.sworddao.phoenix.feature.writing.data.WritingSession
import com.sworddao.phoenix.feature.writing.data.WritingStatistics

@Composable
fun WritingProgressCard(
    session: WritingSession?,
    statistics: WritingStatistics,
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
                text = stringResource(R.string.writing_session_progress),
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
                        stringResource(R.string.writing_xp_earned, session.totalXpEarned),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (statistics.currentStreak > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.writing_streak_label) +
                        " ${statistics.currentStreak} 天",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun WritingCharacterCard(
    exercise: WritingExercise,
    strokesCompleted: Int,
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = exercise.hanzi,
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    contentDescription = exercise.hanzi
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${exercise.character.pinyin} · ${exercise.character.english}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(
                    R.string.writing_stroke_progress,
                    strokesCompleted,
                    exercise.strokeCount,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
fun StrokeDirectionButtons(
    enabled: Boolean,
    onSelect: (StrokeDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StrokeDirection.entries.chunked(3).forEach { rowDirections ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowDirections.forEach { direction ->
                    Button(
                        onClick = { onSelect(direction) },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text(
                            text = direction.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WritingFeedbackCard(
    feedback: EngineStrokeFeedback?,
    modifier: Modifier = Modifier,
) {
    if (feedback == null) return
    val isCorrect = feedback.wasCorrect
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isCorrect) Icons.Default.Create else Icons.Default.Create,
                contentDescription = null,
                tint = if (isCorrect) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = feedback.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun WritingResultCard(
    result: WritingResult,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = result.feedbackMessage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (result.attempt.wasCorrect) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            if (result.attempt.wasCorrect) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.writing_xp_earned, result.xpEarned),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Text(
                    text = stringResource(R.string.writing_next),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun WritingCompletionDialog(
    session: WritingSession,
    statistics: WritingStatistics,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = {
            Text(
                text = stringResource(R.string.writing_complete_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.writing_complete_message),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "✓ ${session.correctAttempts} / ${session.exerciseIds.size} · " +
                        stringResource(R.string.writing_xp_earned, session.totalXpEarned) +
                        " · ${session.totalCorrectStrokes} ${stringResource(R.string.writing_total_strokes)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (statistics.currentStreak > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.writing_streak_continued,
                            statistics.currentStreak,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(
                    text = stringResource(R.string.writing_complete_continue),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}
