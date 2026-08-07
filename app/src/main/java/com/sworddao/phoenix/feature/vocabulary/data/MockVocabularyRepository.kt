package com.sworddao.phoenix.feature.vocabulary.data

import com.sworddao.phoenix.data.seed.VocabularySeedData

import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockVocabularyRepository @Inject constructor() : VocabularyRepository {

    private val _words = MutableStateFlow(createInitialWords())

    override fun getAllWords(): Flow<List<VocabularyWord>> = _words

    override fun getWordById(wordId: String): Flow<VocabularyWord?> =
        _words.map { words -> words.find { it.id == wordId } }

    override fun getWordsByCategory(category: VocabularyCategory): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.category == category } }

    override fun getWordsByMastery(mastery: VocabularyMastery): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.mastery == mastery } }

    override fun getWordsByDifficulty(difficulty: VocabularyDifficulty): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.difficulty == difficulty } }

    override fun getWordsByRegion(regionId: String): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.relatedRegionId == regionId } }

    override fun getWordsByNpc(npcId: String): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.relatedNpcId == npcId } }

    override fun getWordsByQuest(questId: String): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.relatedQuestId == questId } }

    override fun getDiscoveredWords(): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.isDiscovered } }

    override fun getUndiscoveredWords(): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { !it.isDiscovered } }

    override fun getFavorites(): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.isFavorite } }

    override fun getRecentlyLearned(limit: Int): Flow<List<VocabularyWord>> =
        _words.map { words ->
            words.filter { it.isDiscovered }
                .sortedByDescending { it.discoveredAt }
                .take(limit)
        }

    override fun searchWords(query: String): Flow<List<VocabularyWord>> =
        _words.map { words ->
            val lowerQuery = query.lowercase()
            words.filter { word ->
                word.pinyin.lowercase().contains(lowerQuery) ||
                    word.english.lowercase().contains(lowerQuery) ||
                    word.mandarin.contains(query) ||
                    word.hanzi?.contains(query) == true
            }
        }

    override fun getStatistics(): Flow<VocabularyStatistics> =
        _words.map { words ->
            val discovered = words.filter { it.isDiscovered }
            VocabularyStatistics(
                totalWords = words.size,
                discoveredWords = discovered.size,
                masteredWords = words.count { it.mastery == VocabularyMastery.MASTERED },
                favoriteWords = words.count { it.isFavorite },
                wordsByCategory = words.groupBy { it.category }.mapValues { it.value.size },
                wordsByMastery = words.groupBy { it.mastery }.mapValues { it.value.size },
                wordsByDifficulty = words.groupBy { it.difficulty }.mapValues { it.value.size },
                totalReviewed = words.sumOf { it.timesReviewed },
                totalSpoken = words.sumOf { it.timesSpoken },
                totalHeard = words.sumOf { it.timesHeard },
                completionPercentage = if (words.isNotEmpty()) discovered.size.toFloat() / words.size else 0f,
            )
        }

    override fun getCategories(): Flow<List<VocabularyCategory>> =
        MutableStateFlow(VocabularyCategory.entries.toList())

    override fun getProgress(wordId: String): Flow<VocabularyProgress?> =
        _words.map { words ->
            val word = words.find { it.id == wordId }
            word?.let {
                VocabularyProgress(
                    wordId = it.id,
                    mastery = it.mastery,
                    timesReviewed = it.timesReviewed,
                    timesSpoken = it.timesSpoken,
                    timesHeard = it.timesHeard,
                    discoveredAt = it.discoveredAt,
                    isFavorite = it.isFavorite,
                )
            }
        }

    override suspend fun discoverWord(wordId: String): VocabularyResult {
        val words = _words.value
        val word = words.find { it.id == wordId }
            ?: return VocabularyResult.Error("Word not found")

        if (word.isDiscovered) {
            return VocabularyResult.Error("Word already discovered")
        }

        _words.update { wordList ->
            wordList.map { w ->
                if (w.id == wordId) w.copy(
                    discoveredAt = System.currentTimeMillis(),
                    mastery = VocabularyMastery.SEEN,
                ) else w
            }
        }

        return VocabularyResult.WordDiscovered(word.copy(discoveredAt = System.currentTimeMillis()))
    }

    override suspend fun updateMastery(wordId: String, mastery: VocabularyMastery): VocabularyResult {
        val words = _words.value
        val word = words.find { it.id == wordId }
            ?: return VocabularyResult.Error("Word not found")

        _words.update { wordList ->
            wordList.map { w ->
                if (w.id == wordId) w.copy(mastery = mastery) else w
            }
        }

        return VocabularyResult.MasteryUpgraded(word, mastery)
    }

    override suspend fun toggleFavorite(wordId: String): VocabularyResult {
        val words = _words.value
        val word = words.find { it.id == wordId }
            ?: return VocabularyResult.Error("Word not found")

        val newFavorite = !word.isFavorite
        _words.update { wordList ->
            wordList.map { w ->
                if (w.id == wordId) w.copy(isFavorite = newFavorite) else w
            }
        }

        return VocabularyResult.FavoriteToggled(word, newFavorite)
    }

    override suspend fun incrementReview(wordId: String): VocabularyResult {
        _words.update { wordList: List<VocabularyWord> ->
            wordList.map { w: VocabularyWord ->
                if (w.id == wordId) w.copy(timesReviewed = w.timesReviewed + 1) else w
            }
        }
        return VocabularyResult.Success("Review recorded")
    }

    override suspend fun incrementSpoken(wordId: String): VocabularyResult {
        _words.update { wordList: List<VocabularyWord> ->
            wordList.map { w: VocabularyWord ->
                if (w.id == wordId) w.copy(timesSpoken = w.timesSpoken + 1) else w
            }
        }
        return VocabularyResult.Success("Spoken recorded")
    }

    override suspend fun incrementHeard(wordId: String): VocabularyResult {
        _words.update { wordList: List<VocabularyWord> ->
            wordList.map { w: VocabularyWord ->
                if (w.id == wordId) w.copy(timesHeard = w.timesHeard + 1) else w
            }
        }
        return VocabularyResult.Success("Heard recorded")
    }

    override suspend fun incrementRead(wordId: String): VocabularyResult {
        _words.update { wordList: List<VocabularyWord> ->
            wordList.map { w: VocabularyWord ->
                if (w.id == wordId) w.copy(timesRead = w.timesRead + 1) else w
            }
        }
        return VocabularyResult.Success("Read recorded")
    }

    override suspend fun recordDiscovery(wordId: String, source: VocabularySource): VocabularyResult {
        return discoverWord(wordId)
    }

    override suspend fun addWords(words: List<VocabularyWord>): VocabularyResult {
        _words.update { currentWords -> currentWords + words }
        return VocabularyResult.Success("Words added: ${words.size}")
    }

    fun resetVocabularySystem() {
        _words.update { wordList ->
            wordList.map { word ->
                word.copy(
                    discoveredAt = null,
                    mastery = VocabularyMastery.UNKNOWN,
                    timesReviewed = 0,
                    timesSpoken = 0,
                    timesHeard = 0,
                    timesRead = 0,
                    isFavorite = false,
                )
            }
        }
    }

    
    private fun createInitialWords(): List<VocabularyWord> =
        VocabularySeedData.createInitialWords()
}
