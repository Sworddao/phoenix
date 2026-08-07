package com.sworddao.phoenix.feature.vocabulary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {

    @Query("SELECT * FROM vocabulary_word ORDER BY id")
    fun getAllWords(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_word WHERE id = :wordId")
    fun getWordById(wordId: String): Flow<VocabularyEntity?>

    @Query("SELECT * FROM vocabulary_word WHERE category = :category ORDER BY id")
    fun getWordsByCategory(category: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_word WHERE mastery = :mastery ORDER BY id")
    fun getWordsByMastery(mastery: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_word WHERE difficulty = :difficulty ORDER BY id")
    fun getWordsByDifficulty(difficulty: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_word WHERE relatedRegionId = :regionId ORDER BY id")
    fun getWordsByRegion(regionId: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_word WHERE relatedNpcId = :npcId ORDER BY id")
    fun getWordsByNpc(npcId: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_word WHERE relatedQuestId = :questId ORDER BY id")
    fun getWordsByQuest(questId: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_word WHERE discoveredAt IS NOT NULL ORDER BY discoveredAt DESC")
    fun getDiscoveredWords(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_word WHERE discoveredAt IS NULL ORDER BY id")
    fun getUndiscoveredWords(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_word WHERE isFavorite = 1 ORDER BY id")
    fun getFavorites(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary_word WHERE discoveredAt IS NOT NULL ORDER BY discoveredAt DESC LIMIT :limit")
    fun getRecentlyLearned(limit: Int): Flow<List<VocabularyEntity>>

    @Query(
        """
        SELECT * FROM vocabulary_word
        WHERE mandarin LIKE '%' || :query || '%'
           OR english LIKE '%' || :query || '%'
           OR pinyin LIKE '%' || :query || '%'
           OR hanzi LIKE '%' || :query || '%'
        ORDER BY id
        """
    )
    fun searchWords(query: String): Flow<List<VocabularyEntity>>

    @Query("SELECT COUNT(*) FROM vocabulary_word")
    suspend fun countWords(): Int

    @Query("SELECT COUNT(*) FROM vocabulary_word WHERE discoveredAt IS NOT NULL")
    suspend fun countDiscovered(): Int

    @Query("SELECT COUNT(*) FROM vocabulary_word WHERE mastery = :mastery")
    suspend fun countByMastery(mastery: String): Int

    @Query("SELECT COUNT(*) FROM vocabulary_word WHERE isFavorite = 1")
    suspend fun countFavorites(): Int

    @Query("SELECT category, COUNT(*) as count FROM vocabulary_word GROUP BY category")
    suspend fun countByCategory(): List<CategoryCount>

    @Query("SELECT mastery, COUNT(*) as count FROM vocabulary_word GROUP BY mastery")
    suspend fun countByMasteryGroup(): List<MasteryCount>

    @Query("SELECT difficulty, COUNT(*) as count FROM vocabulary_word GROUP BY difficulty")
    suspend fun countByDifficulty(): List<DifficultyCount>

    @Query("SELECT SUM(timesReviewed) FROM vocabulary_word")
    suspend fun sumReviewed(): Long?

    @Query("SELECT SUM(timesSpoken) FROM vocabulary_word")
    suspend fun sumSpoken(): Long?

    @Query("SELECT SUM(timesHeard) FROM vocabulary_word")
    suspend fun sumHeard(): Long?

    @Query("UPDATE vocabulary_word SET discoveredAt = :timestamp, mastery = 'SEEN' WHERE id = :wordId AND discoveredAt IS NULL")
    suspend fun discoverWord(wordId: String, timestamp: Long): Int

    @Query("UPDATE vocabulary_word SET mastery = :mastery WHERE id = :wordId")
    suspend fun updateMastery(wordId: String, mastery: String)

    @Query("UPDATE vocabulary_word SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END WHERE id = :wordId")
    suspend fun toggleFavorite(wordId: String)

    @Query("UPDATE vocabulary_word SET timesReviewed = timesReviewed + 1 WHERE id = :wordId")
    suspend fun incrementReview(wordId: String)

    @Query("UPDATE vocabulary_word SET timesSpoken = timesSpoken + 1 WHERE id = :wordId")
    suspend fun incrementSpoken(wordId: String)

    @Query("UPDATE vocabulary_word SET timesHeard = timesHeard + 1 WHERE id = :wordId")
    suspend fun incrementHeard(wordId: String)

    @Query("UPDATE vocabulary_word SET timesRead = timesRead + 1 WHERE id = :wordId")
    suspend fun incrementRead(wordId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(words: List<VocabularyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(word: VocabularyEntity)

    @Query("DELETE FROM vocabulary_word")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM vocabulary_progress")
    suspend fun countProgress(): Int

    @Query("SELECT * FROM vocabulary_progress WHERE wordId = :wordId")
    fun getProgress(wordId: String): Flow<VocabularyProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: VocabularyProgressEntity)
}

data class CategoryCount(val category: String, val count: Int)
data class MasteryCount(val mastery: String, val count: Int)
data class DifficultyCount(val difficulty: String, val count: Int)
