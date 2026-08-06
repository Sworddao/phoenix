package com.sworddao.phoenix.feature.vocabulary.domain

import com.sworddao.phoenix.feature.vocabulary.data.*
import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    fun getAllWords(): Flow<List<VocabularyWord>>
    fun getWordById(wordId: String): Flow<VocabularyWord?>
    fun getWordsByCategory(category: VocabularyCategory): Flow<List<VocabularyWord>>
    fun getWordsByMastery(mastery: VocabularyMastery): Flow<List<VocabularyWord>>
    fun getWordsByDifficulty(difficulty: VocabularyDifficulty): Flow<List<VocabularyWord>>
    fun getWordsByRegion(regionId: String): Flow<List<VocabularyWord>>
    fun getWordsByNpc(npcId: String): Flow<List<VocabularyWord>>
    fun getWordsByQuest(questId: String): Flow<List<VocabularyWord>>
    fun getDiscoveredWords(): Flow<List<VocabularyWord>>
    fun getUndiscoveredWords(): Flow<List<VocabularyWord>>
    fun getFavorites(): Flow<List<VocabularyWord>>
    fun getRecentlyLearned(limit: Int): Flow<List<VocabularyWord>>
    fun searchWords(query: String): Flow<List<VocabularyWord>>
    fun getStatistics(): Flow<VocabularyStatistics>
    fun getCategories(): Flow<List<VocabularyCategory>>
    fun getProgress(wordId: String): Flow<VocabularyProgress?>

    suspend fun discoverWord(wordId: String): VocabularyResult
    suspend fun updateMastery(wordId: String, mastery: VocabularyMastery): VocabularyResult
    suspend fun toggleFavorite(wordId: String): VocabularyResult
    suspend fun incrementReview(wordId: String): VocabularyResult
    suspend fun incrementSpoken(wordId: String): VocabularyResult
    suspend fun incrementHeard(wordId: String): VocabularyResult
    suspend fun incrementRead(wordId: String): VocabularyResult
    suspend fun recordDiscovery(wordId: String, source: VocabularySource): VocabularyResult
    suspend fun addWords(words: List<VocabularyWord>): VocabularyResult
}
