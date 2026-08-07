package com.sworddao.phoenix.feature.discovery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveryDao {

    @Query("SELECT * FROM vocabulary_discovery ORDER BY discoveredAt DESC")
    fun getAllDiscoveries(): Flow<List<VocabularyDiscoveryEntity>>

    @Query("SELECT * FROM vocabulary_discovery WHERE id = :discoveryId")
    fun getDiscoveryById(discoveryId: String): Flow<VocabularyDiscoveryEntity?>

    @Query("SELECT * FROM vocabulary_discovery WHERE wordId = :wordId ORDER BY discoveredAt DESC")
    fun getDiscoveriesByWord(wordId: String): Flow<List<VocabularyDiscoveryEntity>>

    @Query("SELECT * FROM vocabulary_discovery WHERE source = :source ORDER BY discoveredAt DESC")
    fun getDiscoveriesBySource(source: String): Flow<List<VocabularyDiscoveryEntity>>

    @Query("SELECT * FROM vocabulary_discovery WHERE relatedNpcId = :npcId ORDER BY discoveredAt DESC")
    fun getDiscoveriesByNpc(npcId: String): Flow<List<VocabularyDiscoveryEntity>>

    @Query("SELECT * FROM vocabulary_discovery WHERE relatedQuestId = :questId ORDER BY discoveredAt DESC")
    fun getDiscoveriesByQuest(questId: String): Flow<List<VocabularyDiscoveryEntity>>

    @Query("SELECT * FROM vocabulary_discovery WHERE relatedRegionId = :regionId ORDER BY discoveredAt DESC")
    fun getDiscoveriesByRegion(regionId: String): Flow<List<VocabularyDiscoveryEntity>>

    @Query("SELECT * FROM vocabulary_discovery ORDER BY discoveredAt DESC LIMIT :limit")
    fun getRecentDiscoveries(limit: Int): Flow<List<VocabularyDiscoveryEntity>>

    @Query("SELECT * FROM vocabulary_discovery WHERE discoveredAt >= :start ORDER BY discoveredAt DESC")
    fun getDiscoveriesAfter(start: Long): Flow<List<VocabularyDiscoveryEntity>>

    @Query("SELECT * FROM vocabulary_discovery WHERE wordId = :wordId LIMIT 1")
    fun getDiscoveryByWord(wordId: String): Flow<VocabularyDiscoveryEntity?>

    @Query("SELECT COUNT(*) FROM vocabulary_discovery")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM vocabulary_discovery WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("SELECT COUNT(*) FROM vocabulary_discovery WHERE relatedRegionId = :regionId")
    suspend fun countByRegion(regionId: String): Int

    @Query("SELECT COUNT(*) FROM vocabulary_discovery WHERE wordId = :wordId")
    suspend fun countByWord(wordId: String): Int

    @Query("SELECT * FROM vocabulary_discovery ORDER BY discoveredAt ASC LIMIT 1")
    suspend fun getFirstDiscovery(): VocabularyDiscoveryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(discovery: VocabularyDiscoveryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(discoveries: List<VocabularyDiscoveryEntity>)

    @Query("DELETE FROM vocabulary_discovery")
    suspend fun clearAll()

    @Query("SELECT * FROM discovery_session ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<DiscoverySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: DiscoverySessionEntity)

    @Query("DELETE FROM discovery_session")
    suspend fun clearSessions()
}
