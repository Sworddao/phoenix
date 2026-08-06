package com.sworddao.phoenix.feature.passport.domain

import com.sworddao.phoenix.feature.passport.data.AchievementProgress
import com.sworddao.phoenix.feature.passport.data.Collectible
import com.sworddao.phoenix.feature.passport.data.CollectibleCategory
import com.sworddao.phoenix.feature.passport.data.CollectionProgress
import com.sworddao.phoenix.feature.passport.data.DiscoveryEvent
import com.sworddao.phoenix.feature.passport.data.Passport
import com.sworddao.phoenix.feature.passport.data.PassportEntry
import com.sworddao.phoenix.feature.passport.data.PassportRegion
import com.sworddao.phoenix.feature.passport.data.PassportResult
import kotlinx.coroutines.flow.Flow

interface PassportRepository {
    fun getPassport(): Flow<Passport>
    fun getPassportRegion(regionId: String): Flow<PassportRegion?>
    fun getAllRegions(): Flow<List<PassportRegion>>
    fun getCollectibles(): Flow<List<Collectible>>
    fun getCollectiblesByRegion(regionId: String): Flow<List<Collectible>>
    fun getCollectiblesByCategory(category: CollectibleCategory): Flow<List<Collectible>>
    fun getCollectionProgress(): Flow<CollectionProgress>
    fun getDiscoveryTimeline(): Flow<List<DiscoveryEvent>>
    fun getAchievements(): Flow<List<AchievementProgress>>
    fun getRecentEntries(limit: Int = 10): Flow<List<PassportEntry>>

    suspend fun discoverRegion(regionId: String): PassportResult
    suspend fun completeRegion(regionId: String): PassportResult
    suspend fun earnStamp(regionId: String): PassportResult
    suspend fun collectItem(collectibleId: String): PassportResult
    suspend fun recordEntry(entry: PassportEntry): PassportResult
    suspend fun recordDiscovery(event: DiscoveryEvent): PassportResult
    suspend fun updateRegionProgress(regionId: String, progress: Float): PassportResult
    suspend fun addVocabularyLearned(regionId: String, count: Int): PassportResult
    suspend fun addFriendshipMade(regionId: String): PassportResult
    suspend fun addQuestCompleted(regionId: String): PassportResult
    suspend fun checkAchievements(): List<String>
    suspend fun getPassportStats(): PassportStats
}

data class PassportStats(
    val totalRegions: Int,
    val discoveredRegions: Int,
    val completedRegions: Int,
    val totalStamps: Int,
    val totalCollectibles: Int,
    val collectedItems: Int,
    val totalDiscoveries: Int,
    val totalPlayTimeMinutes: Int,
    val favoriteRegion: String?,
    val rarestCollectible: String?,
    val completionPercentage: Float,
)
