package com.sworddao.phoenix.feature.pronunciation.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.pronunciation.viewmodel.PronunciationViewModel

@Composable
fun PronunciationScreen(
    wordId: String = "",
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PronunciationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val reduceMotion = rememberReducedMotion()

    LaunchedEffect(Unit) {
        viewModel.startPractice(wordId.ifBlank { null })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics {
                contentDescription = context.getString(R.string.pronunciation_screen_accessibility)
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
                    text = stringResource(R.string.pronunciation_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (wordId.isNotEmpty()) {
                        stringResource(R.string.pronunciation_from_vocabulary)
                    } else {
                        stringResource(R.string.pronunciation_from_dialogue)
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
                CompleteContent(
                    uiState = uiState,
                    reduceMotion = reduceMotion,
                    onComplete = onComplete
                )
            }

            else -> {
                PracticeContent(
                    uiState = uiState,
                    reduceMotion = reduceMotion,
                    onDemonstrate = { viewModel.demonstrate() },
                    onRecord = { viewModel.startRecording() },
                    onRepeat = { viewModel.repeatExercise() },
                    onNext = { viewModel.nextExercise() }
                )
            }
        }
    }
}

@Composable
private fun CompleteContent(
    uiState: com.sworddao.phoenix.feature.pronunciation.viewmodel.PronunciationUiState,
    reduceMotion: Boolean,
    onComplete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        uiState.session?.let { session ->
            SpeakingCompleteCard(
                session = session,
                onContinue = onComplete
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.pronunciation_streak_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.pronunciation_streak_continued,
                            uiState.statistics.currentStreak.coerceAtLeast(1)
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "词汇进展 ${uiState.statistics.wordsPracticed}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeContent(
    uiState: com.sworddao.phoenix.feature.pronunciation.viewmodel.PronunciationUiState,
    reduceMotion: Boolean,
    onDemonstrate: () -> Unit,
    onRecord: () -> Unit,
    onRepeat: () -> Unit,
    onNext: () -> Unit,
) {
    val exercise = uiState.currentExercise

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        uiState.session?.let { session ->
            SpeakingProgressCard(
                session = session,
                completedCount = uiState.exercises.indexOf(uiState.currentExercise).coerceAtLeast(0),
                totalCount = uiState.exercises.size,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (exercise != null) {
            PronunciationCard(exercise = exercise)

            Spacer(modifier = Modifier.height(12.dp))

            BaoPronunciationTip(
                exercise = exercise,
                isSpeaking = uiState.isSpeaking,
                reduceMotion = reduceMotion,
                onDemonstrate = onDemonstrate
            )

            Spacer(modifier = Modifier.height(12.dp))

            RecordingIndicator(
                isListening = uiState.isListening,
                reduceMotion = reduceMotion,
                transcript = uiState.transcript
            )

            Spacer(modifier = Modifier.height(20.dp))

            SpeakingButton(
                isRecording = uiState.isRecording,
                reduceMotion = reduceMotion,
                onClick = {
                    if (!uiState.isRecording) {
                        onRecord()
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            uiState.lastResult?.let { result ->
                PronunciationResultCard(
                    result = result,
                    reduceMotion = reduceMotion,
                    onRepeat = onRepeat,
                    onNext = onNext
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}