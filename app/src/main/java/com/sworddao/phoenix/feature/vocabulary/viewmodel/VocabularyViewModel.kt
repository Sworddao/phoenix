package com.sworddao.phoenix.feature.vocabulary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.vocabulary.data.*
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VocabularyUiState(
    val words: List<VocabularyWord> = emptyList(),
    val filteredWords: List<VocabularyWord> = emptyList(),
    val selectedWord: VocabularyWord? = null,
    val statistics: VocabularyStatistics? = null,
    val categories: List<VocabularyCategory> = VocabularyCategory.entries,
    val selectedCategory: VocabularyCategory? = null,
    val selectedMastery: VocabularyMastery? = null,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val showRecentlyLearned: Boolean = false,
    val showMasteredOnly: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDetail: Boolean = false,
)

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val repository: VocabularyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    init {
        loadWords()
    }

    private fun loadWords() {
        viewModelScope.launch {
            combine(
                repository.getAllWords(),
                repository.getStatistics(),
                repository.getCategories(),
            ) { words, stats, categories ->
                VocabularyUiState(
                    words = words,
                    filteredWords = words,
                    statistics = stats,
                    categories = categories,
                    isLoading = false,
                )
            }.collect { state ->
                _uiState.value = state
                applyFilters()
            }
        }
    }

    fun selectWord(word: VocabularyWord) {
        _uiState.update { it.copy(selectedWord = word, showDetail = true) }
        viewModelScope.launch {
            repository.incrementReview(word.id)
        }
    }

    fun clearSelectedWord() {
        _uiState.update { it.copy(selectedWord = null, showDetail = false) }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun filterByCategory(category: VocabularyCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    fun filterByMastery(mastery: VocabularyMastery?) {
        _uiState.update { it.copy(selectedMastery = mastery) }
        applyFilters()
    }

    fun toggleFavoritesOnly() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
        applyFilters()
    }

    fun toggleRecentlyLearned() {
        _uiState.update { it.copy(showRecentlyLearned = !it.showRecentlyLearned) }
        applyFilters()
    }

    fun toggleMasteredOnly() {
        _uiState.update { it.copy(showMasteredOnly = !it.showMasteredOnly) }
        applyFilters()
    }

    fun toggleFavorite(wordId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(wordId)
        }
    }

    fun updateMastery(wordId: String, mastery: VocabularyMastery) {
        viewModelScope.launch {
            repository.updateMastery(wordId, mastery)
        }
    }

    fun discoverWord(wordId: String) {
        viewModelScope.launch {
            repository.discoverWord(wordId)
        }
    }

    fun recordSpoken(wordId: String) {
        viewModelScope.launch {
            repository.incrementSpoken(wordId)
        }
    }

    fun recordHeard(wordId: String) {
        viewModelScope.launch {
            repository.incrementHeard(wordId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.words

        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter { word ->
                word.pinyin.lowercase().contains(query) ||
                    word.english.lowercase().contains(query) ||
                    word.mandarin.contains(state.searchQuery) ||
                    word.hanzi?.contains(state.searchQuery) == true
            }
        }

        if (state.selectedCategory != null) {
            filtered = filtered.filter { it.category == state.selectedCategory }
        }

        if (state.selectedMastery != null) {
            filtered = filtered.filter { it.mastery == state.selectedMastery }
        }

        if (state.showFavoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        if (state.showRecentlyLearned) {
            filtered = filtered.filter { it.isDiscovered }
                .sortedByDescending { it.discoveredAt }
        }

        if (state.showMasteredOnly) {
            filtered = filtered.filter { it.mastery == VocabularyMastery.MASTERED }
        }

        _uiState.update { it.copy(filteredWords = filtered) }
    }
}
