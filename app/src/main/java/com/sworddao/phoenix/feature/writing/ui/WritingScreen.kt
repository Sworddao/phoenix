package com.sworddao.phoenix.feature.writing.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import com.sworddao.phoenix.feature.writing.viewmodel.WritingUiState
import com.sworddao.phoenix.feature.writing.viewmodel.WritingViewModel

@Composable
fun WritingScreen(
    wordId: String = "",
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WritingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startPractice(wordId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics {
                contentDescription = context.getString(R.string.writing_screen_accessibility)
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
                    text = stringResource(R.string.writing_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (wordId.isNotEmpty()) {
                        stringResource(R.string.writing_from_vocabulary)
                    } else {
                        stringResource(R.string.writing_from_dialogue)
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
                    WritingCompletionDialog(
                        session = session,
                        statistics = uiState.statistics,
                        onContinue = onComplete
                    )
                }
            }

            else -> {
                PracticeContent(
                    uiState = uiState,
                    onSelectDirection = { direction -> viewModel.recordStroke(direction) },
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
    uiState: WritingUiState,
    onSelectDirection: (com.sworddao.phoenix.feature.writing.data.StrokeDirection) -> Unit,
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
        WritingProgressCard(
            session = uiState.session,
            statistics = uiState.statistics,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (exercise != null) {
            WritingCharacterCard(
                exercise = exercise,
                strokesCompleted = uiState.strokesCompleted.size,
            )

            Spacer(modifier = Modifier.height(12.dp))

            uiState.lastStrokeFeedback?.let { feedback ->
                WritingFeedbackCard(feedback = feedback)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (lastResult == null) {
                Text(
                    text = stringResource(R.string.writing_choose_direction),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                StrokeDirectionButtons(
                    enabled = !uiState.isExerciseComplete,
                    onSelect = onSelectDirection,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(
                        R.string.writing_expected_stroke,
                        exercise.character.strokes
                            .getOrNull(uiState.expectedStrokeIndex)
                            ?.nameCn ?: "—",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            lastResult?.let { result ->
                Spacer(modifier = Modifier.height(12.dp))
                WritingResultCard(
                    result = result,
                    onNext = onNext,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
