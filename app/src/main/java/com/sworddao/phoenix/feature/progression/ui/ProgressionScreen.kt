package com.sworddao.phoenix.feature.progression.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.sworddao.phoenix.feature.progression.viewmodel.ProgressionUiState
import com.sworddao.phoenix.feature.progression.viewmodel.ProgressionViewModel

@Composable
fun ProgressionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressionViewModel = hiltViewModel(),
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
                contentDescription = context.getString(R.string.progression_screen_accessibility)
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
                    contentDescription = stringResource(R.string.progression_back)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.progression_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.progression_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.progression_refresh)
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
                ProgressionContent(
                    uiState = uiState,
                    onAwardDemoXp = { viewModel.awardXp(com.sworddao.phoenix.feature.progression.data.XpSource.DIALOGUE, 1) },
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
private fun ProgressionContent(
    uiState: ProgressionUiState,
    onAwardDemoXp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PlayerLevelCard(player = uiState.playerProgress)

        Spacer(modifier = Modifier.height(12.dp))

        DailyProgressCard(daily = uiState.dailyProgress)

        Spacer(modifier = Modifier.height(12.dp))

        LearningRadarSummary(learning = uiState.learningProgress)

        Spacer(modifier = Modifier.height(12.dp))

        ChapterProgressSection(chapters = uiState.playerProgress.chapters)

        Spacer(modifier = Modifier.height(12.dp))

        ObjectivesSection(objectives = uiState.currentObjectives)

        Spacer(modifier = Modifier.height(12.dp))

        RecentUnlocksSection(unlocks = uiState.recentUnlocks)

        Spacer(modifier = Modifier.height(12.dp))

        FeatureUnlockTimeline(entries = uiState.featureUnlockTimeline)

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onAwardDemoXp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.progression_demo_xp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}