package com.sworddao.phoenix.feature.discovery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.discovery.data.DiscoveryAnimationState
import com.sworddao.phoenix.feature.discovery.data.DiscoveryHistory
import com.sworddao.phoenix.feature.discovery.data.DiscoveryRepository
import com.sworddao.phoenix.feature.discovery.data.DiscoveryResult
import com.sworddao.phoenix.feature.discovery.data.DiscoverySourceType
import com.sworddao.phoenix.feature.discovery.data.DiscoveryStatistics
import com.sworddao.phoenix.feature.discovery.data.VocabularyDiscovery
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoveryUiState(
    val discoveries: List<VocabularyDiscovery> = emptyList(),
    val filteredDiscoveries: List<VocabularyDiscovery> = emptyList(),
    val statistics: DiscoveryStatistics? = null,
    val history: DiscoveryHistory? = null,
    val recentDiscoveries: List<VocabularyDiscovery> = emptyList(),
    val todayDiscoveries: List<VocabularyDiscovery> = emptyList(),
    val streakDays: Int = 0,
    val selectedDiscovery: VocabularyDiscovery? = null,
    val selectedSource: DiscoverySourceType? = null,
    val selectedCategory: VocabularyCategory? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDiscoveryDialog: Boolean = false,
    val discoveryDialogWord: VocabularyWord? = null,
    val animationState: DiscoveryAnimationState = DiscoveryAnimationState(),
    val lastDiscoveryResult: DiscoveryResult? = null,
)

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val discoveryRepository: DiscoveryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                discoveryRepository.getAllDiscoveries(),
                discoveryRepository.getDiscoveryStatistics(),
                discoveryRepository.getDiscoveryHistory(),
                discoveryRepository.getRecentDiscoveries(10),
                discoveryRepository.getTodayDiscoveries(),
            ) { discoveries, statistics, history, recent, today ->
                DiscoveryUiState(
                    discoveries = discoveries,
                    filteredDiscoveries = applyFilters(
                        discoveries,
                        _uiState.value.selectedSource,
                        _uiState.value.selectedCategory,
                        _uiState.value.searchQuery,
                    ),
                    statistics = statistics,
                    history = history,
                    recentDiscoveries = recent,
                    todayDiscoveries = today,
                    isLoading = false,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        viewModelScope.launch {
            discoveryRepository.getStreakDays().collect { streak ->
                _uiState.update { it.copy(streakDays = streak) }
            }
        }
    }

    fun selectDiscovery(discovery: VocabularyDiscovery) {
        _uiState.update { it.copy(selectedDiscovery = discovery) }
    }

    fun clearSelectedDiscovery() {
        _uiState.update { it.copy(selectedDiscovery = null) }
    }

    fun filterBySource(source: DiscoverySourceType?) {
        _uiState.update { state ->
            state.copy(
                selectedSource = source,
                filteredDiscoveries = applyFilters(
                    state.discoveries,
                    source,
                    state.selectedCategory,
                    state.searchQuery,
                ),
            )
        }
    }

    fun filterByCategory(category: VocabularyCategory?) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredDiscoveries = applyFilters(
                    state.discoveries,
                    state.selectedSource,
                    category,
                    state.searchQuery,
                ),
            )
        }
    }

    fun search(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredDiscoveries = applyFilters(
                    state.discoveries,
                    state.selectedSource,
                    state.selectedCategory,
                    query,
                ),
            )
        }
    }

    fun clearFilters() {
        _uiState.update { state ->
            state.copy(
                selectedSource = null,
                selectedCategory = null,
                searchQuery = "",
                filteredDiscoveries = state.discoveries,
            )
        }
    }

    fun discoverWord(
        wordId: String,
        source: DiscoverySourceType,
        sourceId: String,
        sourceName: String,
        relatedNpcId: String? = null,
        relatedQuestId: String? = null,
        relatedRegionId: String? = null,
    ) {
        viewModelScope.launch {
            val result = discoveryRepository.discoverWord(
                wordId = wordId,
                source = source,
                sourceId = sourceId,
                sourceName = sourceName,
                relatedNpcId = relatedNpcId,
                relatedQuestId = relatedQuestId,
                relatedRegionId = relatedRegionId,
            )

            _uiState.update { it.copy(lastDiscoveryResult = result) }

            when (result) {
                is DiscoveryResult.WordDiscovered -> {
                    showDiscoveryDialog(result.word, result.reward, result.isFirstDiscovery)
                }
                is DiscoveryResult.WordAlreadyDiscovered -> {
                    // Silently ignore or show brief notification
                }
                else -> {}
            }
        }
    }

    fun discoverWords(
        wordIds: List<String>,
        source: DiscoverySourceType,
        sourceId: String,
        sourceName: String,
        relatedNpcId: String? = null,
        relatedQuestId: String? = null,
        relatedRegionId: String? = null,
    ) {
        viewModelScope.launch {
            val result = discoveryRepository.discoverWords(
                wordIds = wordIds,
                source = source,
                sourceId = sourceId,
                sourceName = sourceName,
                relatedNpcId = relatedNpcId,
                relatedQuestId = relatedQuestId,
                relatedRegionId = relatedRegionId,
            )

            _uiState.update { it.copy(lastDiscoveryResult = result) }

            when (result) {
                is DiscoveryResult.BatchDiscovered -> {
                    if (result.words.isNotEmpty()) {
                        val firstWord = result.words.first()
                        showDiscoveryDialog(
                            firstWord.word,
                            firstWord.reward,
                            firstWord.isFirstDiscovery,
                        )
                    }
                }
                else -> {}
            }
        }
    }

    private fun showDiscoveryDialog(
        word: VocabularyWord,
        reward: com.sworddao.phoenix.feature.discovery.data.DiscoveryReward,
        isFirstDiscovery: Boolean,
    ) {
        _uiState.update { state ->
            state.copy(
                showDiscoveryDialog = true,
                discoveryDialogWord = word,
                animationState = DiscoveryAnimationState(
                    isShowing = true,
                    currentWord = word,
                    isFirstDiscovery = isFirstDiscovery,
                    reward = reward,
                    animationPhase = com.sworddao.phoenix.feature.discovery.data.AnimationPhase.WORD_APPEARING,
                ),
            )
        }
    }

    fun dismissDiscoveryDialog() {
        _uiState.update { state ->
            state.copy(
                showDiscoveryDialog = false,
                discoveryDialogWord = null,
                animationState = DiscoveryAnimationState(),
                lastDiscoveryResult = null,
            )
        }
    }

    fun updateAnimationPhase(phase: com.sworddao.phoenix.feature.discovery.data.AnimationPhase) {
        _uiState.update { state ->
            state.copy(
                animationState = state.animationState.copy(animationPhase = phase),
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getSourceDisplayName(source: DiscoverySourceType): String {
        return when (source) {
            DiscoverySourceType.NPC -> "NPC"
            DiscoverySourceType.DIALOGUE -> "Conversation"
            DiscoverySourceType.QUEST -> "Quest"
            DiscoverySourceType.FRIENDSHIP -> "Friendship"
            DiscoverySourceType.REGION -> "Region"
            DiscoverySourceType.PASSPORT -> "Passport"
            DiscoverySourceType.STORY -> "Story"
            DiscoverySourceType.LISTENING -> "Listening"
            DiscoverySourceType.SPEAKING -> "Speaking"
            DiscoverySourceType.MINI_GAME -> "Mini Game"
            DiscoverySourceType.FESTIVAL -> "Festival"
            DiscoverySourceType.HIDDEN -> "Hidden"
            DiscoverySourceType.EXPLORATION -> "Exploration"
        }
    }

    private fun applyFilters(
        discoveries: List<VocabularyDiscovery>,
        source: DiscoverySourceType?,
        category: VocabularyCategory?,
        query: String,
    ): List<VocabularyDiscovery> {
        return discoveries.filter { discovery ->
            val matchesSource = source == null || discovery.source == source
            val matchesCategory = category == null || discovery.word?.category == category
            val matchesQuery = query.isEmpty() ||
                discovery.word?.mandarin?.contains(query, ignoreCase = true) == true ||
                discovery.word?.pinyin?.contains(query, ignoreCase = true) == true ||
                discovery.word?.english?.contains(query, ignoreCase = true) == true ||
                discovery.sourceName.contains(query, ignoreCase = true)

            matchesSource && matchesCategory && matchesQuery
        }.sortedByDescending { it.discoveredAt }
    }
}
