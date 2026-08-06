package com.sworddao.phoenix.feature.reading.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.reading.viewmodel.ReadingUiState
import com.sworddao.phoenix.feature.reading.viewmodel.ReadingViewModel
import com.sworddao.phoenix.ui.components.rememberReducedMotion

@Composable
fun ReadingScreen(
    wordId: String = "",
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    showHanzi: Boolean = false,
    viewModel: ReadingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val reduceMotion = rememberReducedMotion()

    LaunchedEffect(Unit) {
        viewModel.startPractice(wordId, showHanzi)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics {
                contentDescription = context.getString(R.string.reading_screen_accessibility)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.reading_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (wordId.isNotEmpty()) {
                        stringResource(R.string.reading_from_vocabulary)
                    } else {
                        stringResource(R.string.reading_from_dialogue)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.isSessionComplete -> {
                uiState.session?.let { session ->
                    ReadingCompletionDialog(
                        session = session,
                        statistics = uiState.statistics,
                        onContinue = onComplete
                    )
                }
            }

            else -> {
                PracticeContent(
                    uiState = uiState,
                    renderer = viewModel.renderer,
                    reduceMotion = reduceMotion,
                    onReveal = { viewModel.revealHanzi() },
                    onSelectChoice = { choiceId -> viewModel.selectChoice(choiceId) },
                    onNext = { viewModel.nextExercise() }
                )
            }
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.dismissError()
        }
    }
}

@Composable
private fun PracticeContent(
    uiState: ReadingUiState,
    renderer: com.sworddao.phoenix.feature.reading.data.HanziRenderer,
    reduceMotion: Boolean,
    onReveal: () -> Unit,
    onSelectChoice: (String) -> Unit,
    onNext: () -> Unit,
) {
    val exercise = uiState.currentExercise
    val lastResult = uiState.lastResult
    val answeredCorrectly = lastResult?.attempt?.wasCorrect == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ReadingProgressCard(
            session = uiState.session,
            statistics = uiState.statistics,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (exercise != null) {
            ReadingExerciseCard(exercise = exercise)

            Spacer(modifier = Modifier.height(12.dp))

            BaoReadingHint(hintIndex = uiState.exercises.indexOf(exercise))

            Spacer(modifier = Modifier.height(12.dp))

            HanziDisplayCard(
                exercise = exercise,
                renderer = renderer,
                revealMode = uiState.revealMode,
                isHanziRevealed = uiState.isHanziRevealed,
                onReveal = onReveal,
            )

            Spacer(modifier = Modifier.height(8.dp))

            RevealButton(
                isHanziRevealed = uiState.isHanziRevealed,
                onClick = onReveal,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.reading_choose),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            exercise.choices.forEach { choice ->
                ReadingChoiceCard(
                    choice = choice,
                    isSelected = uiState.selectedChoiceId == choice.id,
                    isRevealed = lastResult != null,
                    wasCorrectChoice = lastResult != null &&
                        choice.id == exercise.choices.getOrNull(exercise.correctChoiceIndex)?.id,
                    isEnabled = lastResult == null,
                    onClick = { onSelectChoice(choice.id) },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            lastResult?.let { result ->
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (result.attempt.wasCorrect) {
                                stringResource(R.string.reading_correct)
                            } else {
                                stringResource(R.string.reading_incorrect)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.attempt.wasCorrect) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${exercise.hanzi} · ${exercise.pinyin} · ${exercise.english}",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        if (result.attempt.wasCorrect) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.reading_xp_earned,
                                    result.xpEarned,
                                ),
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
                                text = stringResource(R.string.reading_next),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}