package com.sworddao.phoenix.feature.vocabulary.data

import com.sworddao.phoenix.data.seed.VocabularySeedData
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomVocabularyRepository @Inject constructor(
    private val dao: VocabularyDao,
) : VocabularyRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.countWords() == 0) {
                val words = VocabularySeedData.createInitialWords()
                dao.upsertAll(words.map { it.toEntity() })
                words.forEach { word ->
                    dao.upsertProgress(
                        VocabularyProgress(
                            wordId = word.id,
                            mastery = word.mastery,
                            timesReviewed = word.timesReviewed,
                            timesSpoken = word.timesSpoken,
                            timesHeard = word.timesHeard,
                            timesRead = word.timesRead,
                            timesWritten = word.timesWritten,
                            discoveredAt = word.discoveredAt,
                            isFavorite = word.isFavorite,
                        ).toEntity()
                    )
                }
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    override fun getAllWords(): Flow<List<VocabularyWord>> =
        seededFlow { dao.getAllWords().map { list -> list.map { it.toDomain() } } }

    override fun getWordById(wordId: String): Flow<VocabularyWord?> =
        seededFlow { dao.getWordById(wordId).map { it?.toDomain() } }

    override fun getWordsByCategory(category: VocabularyCategory): Flow<List<VocabularyWord>> =
        seededFlow { dao.getWordsByCategory(category.name).map { list -> list.map { it.toDomain() } } }

    override fun getWordsByMastery(mastery: VocabularyMastery): Flow<List<VocabularyWord>> =
        seededFlow { dao.getWordsByMastery(mastery.name).map { list -> list.map { it.toDomain() } } }

    override fun getWordsByDifficulty(difficulty: VocabularyDifficulty): Flow<List<VocabularyWord>> =
        seededFlow { dao.getWordsByDifficulty(difficulty.name).map { list -> list.map { it.toDomain() } } }

    override fun getWordsByRegion(regionId: String): Flow<List<VocabularyWord>> =
        seededFlow { dao.getWordsByRegion(regionId).map { list -> list.map { it.toDomain() } } }

    override fun getWordsByNpc(npcId: String): Flow<List<VocabularyWord>> =
        seededFlow { dao.getWordsByNpc(npcId).map { list -> list.map { it.toDomain() } } }

    override fun getWordsByQuest(questId: String): Flow<List<VocabularyWord>> =
        seededFlow { dao.getWordsByQuest(questId).map { list -> list.map { it.toDomain() } } }

    override fun getDiscoveredWords(): Flow<List<VocabularyWord>> =
        seededFlow { dao.getDiscoveredWords().map { list -> list.map { it.toDomain() } } }

    override fun getUndiscoveredWords(): Flow<List<VocabularyWord>> =
        seededFlow { dao.getUndiscoveredWords().map { list -> list.map { it.toDomain() } } }

    override fun getFavorites(): Flow<List<VocabularyWord>> =
        seededFlow { dao.getFavorites().map { list -> list.map { it.toDomain() } } }

    override fun getRecentlyLearned(limit: Int): Flow<List<VocabularyWord>> =
        seededFlow { dao.getRecentlyLearned(limit).map { list -> list.map { it.toDomain() } } }

    override fun searchWords(query: String): Flow<List<VocabularyWord>> =
        seededFlow { dao.searchWords(query).map { list -> list.map { it.toDomain() } } }

    override fun getStatistics(): Flow<VocabularyStatistics> = seededFlow {
        flow {
            val total = dao.countWords()
            val discovered = dao.countDiscovered()
            val mastered = dao.countByMastery(VocabularyMastery.MASTERED.name)
            val favorites = dao.countFavorites()
            val byCategory = dao.countByCategory().associate { row ->
                runCatching { VocabularyCategory.valueOf(row.category) }.getOrNull() to row.count
            }.filterKeys { it != null }.mapKeys { it.key!! }
            val byMastery = dao.countByMasteryGroup().associate { row ->
                runCatching { VocabularyMastery.valueOf(row.mastery) }.getOrNull() to row.count
            }.filterKeys { it != null }.mapKeys { it.key!! }
            val byDifficulty = dao.countByDifficulty().associate { row ->
                runCatching { VocabularyDifficulty.valueOf(row.difficulty) }.getOrNull() to row.count
            }.filterKeys { it != null }.mapKeys { it.key!! }
            emit(
                VocabularyStatistics(
                    totalWords = total,
                    discoveredWords = discovered,
                    masteredWords = mastered,
                    favoriteWords = favorites,
                    wordsByCategory = byCategory,
                    wordsByMastery = byMastery,
                    wordsByDifficulty = byDifficulty,
                    totalReviewed = (dao.sumReviewed() ?: 0).toInt(),
                    totalSpoken = (dao.sumSpoken() ?: 0).toInt(),
                    totalHeard = (dao.sumHeard() ?: 0).toInt(),
                    completionPercentage = if (total > 0) discovered.toFloat() / total else 0f,
                )
            )
        }
    }

    override fun getCategories(): Flow<List<VocabularyCategory>> =
        seededFlow { flow { emit(VocabularyCategory.entries) } }

    override fun getProgress(wordId: String): Flow<VocabularyProgress?> =
        seededFlow { dao.getProgress(wordId).map { it?.toDomain() } }

    override suspend fun discoverWord(wordId: String): VocabularyResult {
        ensureSeeded()
        val word = dao.getWordById(wordId).first()
            ?: return VocabularyResult.Error("Word not found")
        if (word.discoveredAt != null) {
            return VocabularyResult.Error("Word already discovered")
        }
        dao.discoverWord(wordId, System.currentTimeMillis())
        val updated = dao.getWordById(wordId).first()
        return VocabularyResult.WordDiscovered(updated?.toDomain() ?: word.toDomain())
    }

    override suspend fun updateMastery(wordId: String, mastery: VocabularyMastery): VocabularyResult {
        ensureSeeded()
        val word = dao.getWordById(wordId).first()
            ?: return VocabularyResult.Error("Word not found")
        dao.updateMastery(wordId, mastery.name)
        return VocabularyResult.MasteryUpgraded(word.toDomain(), mastery)
    }

    override suspend fun toggleFavorite(wordId: String): VocabularyResult {
        ensureSeeded()
        val word = dao.getWordById(wordId).first()
            ?: return VocabularyResult.Error("Word not found")
        val newFavorite = !word.isFavorite
        dao.toggleFavorite(wordId)
        return VocabularyResult.FavoriteToggled(word.toDomain(), newFavorite)
    }

    override suspend fun incrementReview(wordId: String): VocabularyResult {
        ensureSeeded()
        dao.incrementReview(wordId)
        return VocabularyResult.Success("Review recorded")
    }

    override suspend fun incrementSpoken(wordId: String): VocabularyResult {
        ensureSeeded()
        dao.incrementSpoken(wordId)
        return VocabularyResult.Success("Spoken recorded")
    }

    override suspend fun incrementHeard(wordId: String): VocabularyResult {
        ensureSeeded()
        dao.incrementHeard(wordId)
        return VocabularyResult.Success("Heard recorded")
    }

    override suspend fun incrementRead(wordId: String): VocabularyResult {
        ensureSeeded()
        dao.incrementRead(wordId)
        return VocabularyResult.Success("Read recorded")
    }

    override suspend fun incrementWritten(wordId: String): VocabularyResult {
        ensureSeeded()
        dao.incrementWritten(wordId)
        return VocabularyResult.Success("Written recorded")
    }

    override suspend fun recordDiscovery(wordId: String, source: VocabularySource): VocabularyResult {
        return discoverWord(wordId)
    }

    override suspend fun addWords(words: List<VocabularyWord>): VocabularyResult {
        ensureSeeded()
        dao.upsertAll(words.map { it.toEntity() })
        return VocabularyResult.Success("Words added: ${words.size}")
    }
}
