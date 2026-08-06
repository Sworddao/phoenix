package com.sworddao.phoenix.feature.quest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sworddao.phoenix.feature.quest.data.QuestCategory
import com.sworddao.phoenix.feature.quest.data.QuestDifficulty
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.quest.viewmodel.QuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestListScreen(
    onQuestClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: QuestViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<QuestStatus?>(null) }
    var selectedDifficulty by remember { mutableStateOf<QuestDifficulty?>(null) }
    var selectedCategory by remember { mutableStateOf<QuestCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search bar
            TextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    viewModel.setSearchQuery(query)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索任务...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "清除",
                            )
                        }
                    }
                },
                singleLine = true,
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedStatus != null,
                    onClick = {
                        selectedStatus = if (selectedStatus != null) null else QuestStatus.ACTIVE
                        viewModel.filterByStatus(selectedStatus)
                    },
                    label = { Text("进行中") },
                )
                FilterChip(
                    selected = selectedDifficulty != null,
                    onClick = {
                        selectedDifficulty = if (selectedDifficulty != null) null else QuestDifficulty.EASY
                        viewModel.filterByDifficulty(selectedDifficulty)
                    },
                    label = { Text("简单") },
                )
                FilterChip(
                    selected = selectedCategory != null,
                    onClick = {
                        selectedCategory = if (selectedCategory != null) null else QuestCategory.STORY
                        viewModel.filterByCategory(selectedCategory)
                    },
                    label = { Text("故事") },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quest list
            if (uiState.isLoading) {
                Text(
                    text = "加载中...",
                    modifier = Modifier.padding(16.dp),
                )
            } else if (uiState.quests.isEmpty()) {
                Text(
                    text = "没有找到任务",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.quests) { quest ->
                        QuestCard(
                            quest = quest,
                            onClick = { onQuestClick(quest.id) },
                        )
                    }
                }
            }
        }
    }
}
