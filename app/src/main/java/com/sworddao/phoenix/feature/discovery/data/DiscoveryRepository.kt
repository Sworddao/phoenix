package com.sworddao.phoenix.feature.discovery.data

import com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import kotlinx.coroutines.flow.Flow

interface DiscoveryRepository {

    fun getAllDiscoveries(): Flow<List<VocabularyDiscovery>>

    fun getDiscoveryById(discoveryId: String): Flow<VocabularyDiscovery?>

    fun getDiscoveriesByWord(wordId: String): Flow<List<VocabularyDiscovery>>

    fun getDiscoveriesBySource(source: DiscoverySourceType): Flow<List<VocabularyDiscovery>>

    fun getDiscoveriesByNpc(npcId: String): Flow<List<VocabularyDiscovery>>

    fun getDiscoveriesByQuest(questId: String): Flow<List<VocabularyDiscovery>>

    fun getDiscoveriesByRegion(regionId: String): Flow<List<VocabularyDiscovery>>

    fun getDiscoveriesByCategory(category: VocabularyCategory): Flow<List<VocabularyDiscovery>>

    fun getRecentDiscoveries(limit: Int): Flow<List<VocabularyDiscovery>>

    fun getTodayDiscoveries(): Flow<List<VocabularyDiscovery>>

    fun getWeekDiscoveries(): Flow<List<VocabularyDiscovery>>

    fun getDiscoveryHistory(): Flow<DiscoveryHistory>

    fun getDiscoveryStatistics(): Flow<DiscoveryStatistics>

    fun getDiscoverySessions(): Flow<List<DiscoverySession>>

    fun getDailyDiscoveries(): Flow<List<DailyDiscovery>>

    fun getStreakDays(): Flow<Int>

    suspend fun discoverWord(
        wordId: String,
        source: DiscoverySourceType,
        sourceId: String,
        sourceName: String,
        relatedNpcId: String? = null,
        relatedQuestId: String? = null,
        relatedRegionId: String? = null,
    ): DiscoveryResult

    suspend fun discoverWords(
        wordIds: List<String>,
        source: DiscoverySourceType,
        sourceId: String,
        sourceName: String,
        relatedNpcId: String? = null,
        relatedQuestId: String? = null,
        relatedRegionId: String? = null,
    ): DiscoveryResult

    suspend fun isWordDiscovered(wordId: String): Boolean

    suspend fun getDiscoveryCount(): Int

    suspend fun getDiscoveryCountBySource(source: DiscoverySourceType): Int

    suspend fun getDiscoveryCountByRegion(regionId: String): Int

    suspend fun getDiscoveryCountByCategory(category: VocabularyCategory): Int

    suspend fun resetDailyStreak()

    suspend fun recordDiscoverySession(session: DiscoverySession)

    suspend fun clearDiscoveryHistory()
}
