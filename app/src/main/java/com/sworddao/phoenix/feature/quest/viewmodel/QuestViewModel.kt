package com.sworddao.phoenix.feature.quest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.quest.data.Quest
import com.sworddao.phoenix.feature.quest.data.QuestCategory
import com.sworddao.phoenix.feature.quest.data.QuestDifficulty
import com.sworddao.phoenix.feature.quest.data.QuestFilter
import com.sworddao.phoenix.feature.quest.data.QuestResult
import com.sworddao.phoenix.feature.quest.data.QuestStats
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.quest.data.QuestType
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestUiState(
    val quests: List<Quest> = emptyList(),
    val activeQuests: List<Quest> = emptyList(),
    val completedQuests: List<Quest> = emptyList(),
    val availableQuests: List<Quest> = emptyList(),
    val selectedQuest: Quest? = null,
    val stats: QuestStats? = null,
    val filter: QuestFilter = QuestFilter(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showCompletionDialog: Boolean = false,
    val completedQuestResult: QuestResult? = null,
)

@HiltViewModel
class QuestViewModel @Inject constructor(
    private val questRepository: QuestRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestUiState())
    val uiState: StateFlow<QuestUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(QuestFilter())

    val filteredQuests: StateFlow<List<Quest>> = combine(
        questRepository.getAllQuests(),
        _filter,
    ) { quests, filter ->
        quests.filter { quest ->
            (filter.types.isEmpty() || filter.types.contains(quest.type)) &&
                (filter.difficulties.isEmpty() || filter.difficulties.contains(quest.difficulty)) &&
                (filter.statuses.isEmpty() || filter.statuses.contains(quest.status)) &&
                (filter.categories.isEmpty() || filter.categories.contains(quest.category)) &&
                (filter.searchQuery.isEmpty() ||
                    quest.title.contains(filter.searchQuery, ignoreCase = true) ||
                    quest.description.contains(filter.searchQuery, ignoreCase = true))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    init {
        loadQuests()
    }

    private fun loadQuests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                combine(
                    questRepository.getAllQuests(),
                    questRepository.getActiveQuests(),
                    questRepository.getCompletedQuests(),
                    questRepository.getAvailableQuests(),
                    questRepository.getQuestStats(),
                    _filter,
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    val allQuests = values[0] as List<Quest>
                    val active = values[1] as List<Quest>
                    val completed = values[2] as List<Quest>
                    val available = values[3] as List<Quest>
                    val stats = values[4] as QuestStats
                    val filter = values[5] as QuestFilter

                    val filtered = allQuests.filter { quest ->
                        (filter.types.isEmpty() || filter.types.contains(quest.type)) &&
                            (filter.difficulties.isEmpty() || filter.difficulties.contains(quest.difficulty)) &&
                            (filter.statuses.isEmpty() || filter.statuses.contains(quest.status)) &&
                            (filter.categories.isEmpty() || filter.categories.contains(quest.category)) &&
                            (filter.searchQuery.isEmpty() ||
                                quest.title.contains(filter.searchQuery, ignoreCase = true) ||
                                quest.description.contains(filter.searchQuery, ignoreCase = true))
                    }

                    QuestUiState(
                        quests = filtered,
                        activeQuests = active,
                        completedQuests = completed,
                        availableQuests = available,
                        stats = stats,
                        filter = filter,
                        isLoading = false,
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load quests",
                )
            }
        }
    }

    fun selectQuest(quest: Quest) {
        _uiState.value = _uiState.value.copy(selectedQuest = quest)
    }

    fun clearSelectedQuest() {
        _uiState.value = _uiState.value.copy(selectedQuest = null)
    }

    fun startQuest(questId: String) {
        viewModelScope.launch {
            when (val result = questRepository.startQuest(questId)) {
                is QuestResult.Success -> {
                    _uiState.value = _uiState.value.copy(error = null)
                }
                is QuestResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun completeQuest(questId: String) {
        viewModelScope.launch {
            when (val result = questRepository.completeQuest(questId)) {
                is QuestResult.QuestCompleted -> {
                    _uiState.value = _uiState.value.copy(
                        showCompletionDialog = true,
                        completedQuestResult = result,
                    )
                }
                is QuestResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun abandonQuest(questId: String) {
        viewModelScope.launch {
            when (val result = questRepository.abandonQuest(questId)) {
                is QuestResult.Success -> {
                    _uiState.value = _uiState.value.copy(error = null)
                }
                is QuestResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun updateObjectiveProgress(questId: String, objectiveId: String, progress: Int) {
        viewModelScope.launch {
            questRepository.updateObjectiveProgress(questId, objectiveId, progress)
        }
    }

    fun dismissCompletionDialog() {
        _uiState.value = _uiState.value.copy(
            showCompletionDialog = false,
            completedQuestResult = null,
        )
    }

    fun updateFilter(filter: QuestFilter) {
        _filter.value = filter
    }

    fun clearFilter() {
        _filter.value = QuestFilter()
    }

    fun setSearchQuery(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun filterByType(type: QuestType?) {
        _filter.value = _filter.value.copy(
            types = if (type != null) listOf(type) else emptyList(),
        )
    }

    fun filterByDifficulty(difficulty: QuestDifficulty?) {
        _filter.value = _filter.value.copy(
            difficulties = if (difficulty != null) listOf(difficulty) else emptyList(),
        )
    }

    fun filterByStatus(status: QuestStatus?) {
        _filter.value = _filter.value.copy(
            statuses = if (status != null) listOf(status) else emptyList(),
        )
    }

    fun filterByCategory(category: QuestCategory?) {
        _filter.value = _filter.value.copy(
            categories = if (category != null) listOf(category) else emptyList(),
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
