package com.sworddao.phoenix.feature.vocabulary.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.feature.vocabulary.data.*
import com.sworddao.phoenix.feature.vocabulary.viewmodel.VocabularyUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    onBack: () -> Unit,
    onWordClick: (VocabularyWord) -> Unit,
    uiState: VocabularyUiState,
    onSearch: (String) -> Unit,
    onCategoryFilter: (VocabularyCategory?) -> Unit,
    onMasteryFilter: (VocabularyMastery?) -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleRecentlyLearned: () -> Unit,
    onToggleMastered: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("词汇") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    VocabularyStatisticsCard(statistics = uiState.statistics)
                }

                item {
                    VocabularySearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = onSearch
                    )
                }

                item {
                    FilterChipsRow(
                        selectedCategory = uiState.selectedCategory,
                        selectedMastery = uiState.selectedMastery,
                        showFavoritesOnly = uiState.showFavoritesOnly,
                        showRecentlyLearned = uiState.showRecentlyLearned,
                        showMasteredOnly = uiState.showMasteredOnly,
                        onCategoryFilter = onCategoryFilter,
                        onMasteryFilter = onMasteryFilter,
                        onToggleFavorites = onToggleFavorites,
                        onToggleRecentlyLearned = onToggleRecentlyLearned,
                        onToggleMastered = onToggleMastered,
                    )
                }

                item {
                    Text(
                        text = "共 ${uiState.filteredWords.size} 个词汇",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(uiState.filteredWords) { word ->
                    VocabularyCard(
                        word = word,
                        onClick = { onWordClick(word) }
                    )
                }
            }
        }
    }
}

@Composable
fun VocabularyStatisticsCard(statistics: VocabularyStatistics?) {
    if (statistics == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "学习统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = statistics.totalWords.toString(), label = "总词汇", icon = Icons.Default.MenuBook)
                StatItem(value = statistics.discoveredWords.toString(), label = "已学", icon = Icons.Default.Visibility)
                StatItem(value = statistics.masteredWords.toString(), label = "已掌握", icon = Icons.Default.Star)
                StatItem(value = statistics.favoriteWords.toString(), label = "收藏", icon = Icons.Default.Favorite)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { statistics.completionPercentage },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "完成度 ${(statistics.completionPercentage * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun VocabularySearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("搜索词汇 (拼音/英文/汉字)...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "清除")
                }
            }
        },
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipsRow(
    selectedCategory: VocabularyCategory?,
    selectedMastery: VocabularyMastery?,
    showFavoritesOnly: Boolean,
    showRecentlyLearned: Boolean,
    showMasteredOnly: Boolean,
    onCategoryFilter: (VocabularyCategory?) -> Unit,
    onMasteryFilter: (VocabularyMastery?) -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleRecentlyLearned: () -> Unit,
    onToggleMastered: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = showFavoritesOnly,
                onClick = onToggleFavorites,
                label = { Text("收藏") },
                leadingIcon = if (showFavoritesOnly) {{ Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }} else null
            )
            FilterChip(
                selected = showRecentlyLearned,
                onClick = onToggleRecentlyLearned,
                label = { Text("最近学习") },
                leadingIcon = if (showRecentlyLearned) {{ Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp)) }} else null
            )
            FilterChip(
                selected = showMasteredOnly,
                onClick = onToggleMastered,
                label = { Text("已掌握") },
                leadingIcon = if (showMasteredOnly) {{ Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp)) }} else null
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategoryFilter(null) },
                label = { Text("全部") }
            )
            VocabularyCategory.entries.take(4).forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryFilter(if (selectedCategory == category) null else category) },
                    label = { Text(category.displayName) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyCard(word: VocabularyWord, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.pinyin,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (word.isFavorite) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "收藏",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = word.english,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (word.isDiscovered) {
                    Text(
                        text = word.mandarin,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            MasteryIndicator(mastery = word.mastery)
        }
    }
}

@Composable
fun MasteryIndicator(mastery: VocabularyMastery) {
    val (color, label) = when (mastery) {
        VocabularyMastery.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant to "未学"
        VocabularyMastery.SEEN -> MaterialTheme.colorScheme.secondary to "已见"
        VocabularyMastery.LEARNING -> MaterialTheme.colorScheme.tertiary to "学习中"
        VocabularyMastery.FAMILIAR -> MaterialTheme.colorScheme.primary to "熟悉"
        VocabularyMastery.MASTERED -> MaterialTheme.colorScheme.primaryContainer to "已掌握"
    }

    Surface(
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyDetailScreen(
    word: VocabularyWord,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onUpdateMastery: (String, VocabularyMastery) -> Unit,
    onPractice: ((String) -> Unit)? = null,
    onPracticeListening: ((String) -> Unit)? = null,
    onPracticeReading: ((String) -> Unit)? = null,
    onPracticeWriting: ((String) -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(word.pinyin) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onToggleFavorite(word.id) }) {
                        Icon(
                            if (word.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (word.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = word.pinyin,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = word.english,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (word.isDiscovered && word.hanzi != null) {
                            Text(
                                text = word.hanzi,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            CategoryChip(category = word.category)
                            Spacer(modifier = Modifier.width(8.dp))
                            DifficultyChip(difficulty = word.difficulty)
                            Spacer(modifier = Modifier.width(8.dp))
                            MasteryIndicator(mastery = word.mastery)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("例句", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(word.exampleSentence, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            word.exampleTranslation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            word.examplePinyin,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (word.isDiscovered && onPractice != null) {
                item {
                    Button(
                        onClick = { onPractice(word.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "练习发音",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (word.isDiscovered && onPracticeListening != null) {
                item {
                    Button(
                        onClick = { onPracticeListening(word.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "练习聆听",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (word.isDiscovered && onPracticeReading != null) {
                item {
                    Button(
                        onClick = { onPracticeReading(word.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "练习阅读",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (word.isDiscovered && onPracticeWriting != null) {
                item {
                    Button(
                        onClick = { onPracticeWriting(word.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "练习书写",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (word.isDiscovered) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("学习统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(word.timesReviewed.toString(), style = MaterialTheme.typography.titleMedium)
                                    Text("复习", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(word.timesSpoken.toString(), style = MaterialTheme.typography.titleMedium)
                                    Text("口语", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(word.timesHeard.toString(), style = MaterialTheme.typography.titleMedium)
                                    Text("听力", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(word.timesRead.toString(), style = MaterialTheme.typography.titleMedium)
                                    Text("阅读", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(word.timesWritten.toString(), style = MaterialTheme.typography.titleMedium)
                                    Text("书写", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("掌握程度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VocabularyMastery.entries.forEach { mastery ->
                                FilterChip(
                                    selected = word.mastery == mastery,
                                    onClick = { onUpdateMastery(word.id, mastery) },
                                    label = { Text(mastery.displayName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(category: VocabularyCategory) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category.icon, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(category.displayName, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun DifficultyChip(difficulty: VocabularyDifficulty) {
    val color = when (difficulty) {
        VocabularyDifficulty.BEGINNER -> MaterialTheme.colorScheme.tertiaryContainer
        VocabularyDifficulty.ELEMENTARY -> MaterialTheme.colorScheme.secondaryContainer
        VocabularyDifficulty.INTERMEDIATE -> MaterialTheme.colorScheme.primaryContainer
        VocabularyDifficulty.UPPER_INTERMEDIATE -> MaterialTheme.colorScheme.errorContainer
        VocabularyDifficulty.ADVANCED -> MaterialTheme.colorScheme.errorContainer
    }

    Surface(
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = difficulty.displayName,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
