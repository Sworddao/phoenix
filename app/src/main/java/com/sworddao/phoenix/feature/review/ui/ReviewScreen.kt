package com.sworddao.phoenix.feature.review.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.review.data.ReviewType
import com.sworddao.phoenix.feature.review.viewmodel.ReviewUiState
import com.sworddao.phoenix.feature.review.viewmodel.ReviewViewModel

private val selectableReviewTypes = ReviewType.entries.toList()

@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    onOpenSession: (ReviewType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics {
                contentDescription = context.getString(R.string.review_screen_accessibility)
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
                    contentDescription = stringResource(R.string.review_back)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.review_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.review_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.review_refresh)
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

            else -> {
                ReviewDashboardContent(
                    uiState = uiState,
                    onStartType = onOpenSession,
                    onStartDaily = { onOpenSession(ReviewType.DAILY_REVIEW) },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ReviewDashboardContent(
    uiState: ReviewUiState,
    onStartType: (ReviewType) -> Unit,
    onStartDaily: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TodayReviewCard(
            daily = uiState.dailyReview,
            dueCount = uiState.dailyReview.dueCount,
            onStartDaily = onStartDaily,
        )

        Spacer(modifier = Modifier.height(12.dp))

        BaoRecommendationCard(recommendation = uiState.recommendations.firstOrNull())

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            selectableReviewTypes.forEach { type ->
                ReviewTypeChip(
                    type = type,
                    selected = false,
                    onClick = { onStartType(type) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ReviewStatisticsSection(statistics = uiState.statistics)

        Spacer(modifier = Modifier.height(12.dp))

        UpcomingReviewsSection(items = uiState.upcomingReviews)

        Spacer(modifier = Modifier.height(12.dp))

        MemoryStrengthSection(strengths = uiState.memoryStrengths)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ReviewSessionScreen(
    type: ReviewType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.activeSession

    LaunchedEffect(Unit) {
        if (session == null) {
            viewModel.startSession(type)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            session == null && uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            session == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "🐾", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.error ?: stringResource(R.string.review_no_due),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text(text = stringResource(R.string.review_continue))
                    }
                }
            }

            else -> {
                val currentIndex = uiState.sessionAnswers.coerceIn(0, session.totalCount - 1)
                val currentItem = session.items[currentIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.review_back)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.review_session_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ReviewPromptCard(
                        item = currentItem,
                        index = currentIndex,
                        total = session.totalCount,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    ReviewAnswerButtons(
                        onRemember = { viewModel.answerCurrent(true) },
                        onForgot = { viewModel.answerCurrent(false) },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.completeActiveSession() },
                        enabled = session.answeredCount == session.totalCount,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.review_done))
                    }
                }
            }
        }
    }

    if (uiState.showCompletion) {
        ReviewCompletionDialog(
            accuracy = uiState.completedAccuracy,
            xpEarned = uiState.completedXp,
            answered = session?.answeredCount ?: 0,
            total = session?.totalCount ?: 0,
            onDismiss = {
                viewModel.dismissCompletion()
                onBack()
            },
        )
    }
}