package com.sworddao.phoenix.feature.passport.data

import com.sworddao.phoenix.data.seed.PassportSeedData

import com.sworddao.phoenix.feature.passport.domain.PassportRepository
import com.sworddao.phoenix.feature.passport.domain.PassportStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockPassportRepository @Inject constructor() : PassportRepository {

    private val _passport = MutableStateFlow(createInitialPassport())
    private val _regions = MutableStateFlow(createInitialRegions())
    private val _collectibles = MutableStateFlow(createInitialCollectibles())
    private val _timeline = MutableStateFlow(createInitialTimeline())
    private val _achievements = MutableStateFlow(createInitialAchievements())
    private val _entries = MutableStateFlow<List<PassportEntry>>(emptyList())

    override fun getPassport(): Flow<Passport> = _passport

    override fun getPassportRegion(regionId: String): Flow<PassportRegion?> =
        _regions.map { regions -> regions.find { it.regionId == regionId } }

    override fun getAllRegions(): Flow<List<PassportRegion>> = _regions

    override fun getCollectibles(): Flow<List<Collectible>> = _collectibles

    override fun getCollectiblesByRegion(regionId: String): Flow<List<Collectible>> =
        _collectibles.map { collectibles -> collectibles.filter { it.regionId == regionId } }

    override fun getCollectiblesByCategory(category: CollectibleCategory): Flow<List<Collectible>> =
        _collectibles.map { collectibles -> collectibles.filter { it.category == category } }

    override fun getCollectionProgress(): Flow<CollectionProgress> = _collectibles.map { collectibles ->
        val collected = collectibles.filter { it.isCollected }
        val categoryProgress = collected.groupBy { it.category }.mapValues { it.value.size }
        val rarityProgress = collected.groupBy { it.rarity }.mapValues { it.value.size }
        val regionProgress = collected.groupBy { it.regionId }.mapValues { it.value.size }

        CollectionProgress(
            totalCollectibles = collectibles.size,
            collectedCount = collected.size,
            categoryProgress = categoryProgress,
            rarityProgress = rarityProgress,
            regionProgress = regionProgress,
            completionPercentage = if (collectibles.isNotEmpty()) collected.size.toFloat() / collectibles.size else 0f,
            missingCollectibles = collectibles.filter { !it.isCollected }.map { it.id },
            recentCollectibles = collected.sortedByDescending { it.collectedAt }.take(5).map { it.id },
        )
    }

    override fun getDiscoveryTimeline(): Flow<List<DiscoveryEvent>> = _timeline

    override fun getAchievements(): Flow<List<AchievementProgress>> = _achievements

    override fun getRecentEntries(limit: Int): Flow<List<PassportEntry>> =
        _passport.map { passport ->
            val regionEntries = passport.regions.values
                .filter { it.isDiscovered }
                .sortedByDescending { it.discoveredAt }
                .map { region ->
                    PassportEntry(
                        id = "entry_${region.regionId}",
                        regionId = region.regionId,
                        type = EntryType.REGION_DISCOVERED,
                        title = "发现 ${region.regionName}",
                        description = "到达了 ${region.regionNameCn}",
                        timestamp = region.discoveredAt ?: System.currentTimeMillis(),
                    )
                }
            (_entries.value + regionEntries)
                .sortedByDescending { it.timestamp }
                .take(limit)
        }

    override suspend fun discoverRegion(regionId: String): PassportResult {
        val regions = _regions.value
        val region = regions.find { it.regionId == regionId }
            ?: return PassportResult.Error("Region not found")

        if (region.isDiscovered) {
            return PassportResult.Error("Region already discovered")
        }

        _regions.update { regionList ->
            regionList.map { r ->
                if (r.regionId == regionId) r.copy(
                    isDiscovered = true,
                    discoveredAt = System.currentTimeMillis(),
                ) else r
            }
        }

        val event = DiscoveryEvent(
            id = "discovery_${regionId}_${System.currentTimeMillis()}",
            type = EntryType.REGION_DISCOVERED,
            title = "发现新区域",
            description = "发现了 ${region.regionName} (${region.regionNameCn})",
            regionId = regionId,
        )
        _timeline.update { it + event }

        _passport.update { passport ->
            passport.copy(
                totalDiscoveries = passport.totalDiscoveries + 1,
                lastUpdated = System.currentTimeMillis(),
            )
        }

        return PassportResult.Success("Region discovered: ${region.regionName}")
    }

    override suspend fun completeRegion(regionId: String): PassportResult {
        val regions = _regions.value
        val region = regions.find { it.regionId == regionId }
            ?: return PassportResult.Error("Region not found")

        if (!region.isDiscovered) {
            return PassportResult.Error("Region not discovered yet")
        }

        if (region.isCompleted) {
            return PassportResult.Error("Region already completed")
        }

        _regions.update { regionList ->
            regionList.map { r ->
                if (r.regionId == regionId) r.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                    completionPercentage = 100f,
                ) else r
            }
        }

        val event = DiscoveryEvent(
            id = "completion_${regionId}_${System.currentTimeMillis()}",
            type = EntryType.REGION_COMPLETED,
            title = "区域完成",
            description = "完成了 ${region.regionName} 的探索",
            regionId = regionId,
        )
        _timeline.update { it + event }

        return PassportResult.RegionCompleted(regionId)
    }

    override suspend fun earnStamp(regionId: String): PassportResult {
        val regions = _regions.value
        val region = regions.find { it.regionId == regionId }
            ?: return PassportResult.Error("Region not found")

        if (region.stampEarned) {
            return PassportResult.Error("Stamp already earned")
        }

        val rarity = when {
            region.completionPercentage >= 100f -> StampRarity.PLATINUM
            region.completionPercentage >= 80f -> StampRarity.GOLD
            region.completionPercentage >= 60f -> StampRarity.SILVER
            else -> StampRarity.BRONZE
        }

        _regions.update { regionList ->
            regionList.map { r ->
                if (r.regionId == regionId) r.copy(
                    stampEarned = true,
                    stampRarity = rarity,
                ) else r
            }
        }

        _passport.update { passport ->
            passport.copy(
                totalStamps = passport.totalStamps + 1,
                lastUpdated = System.currentTimeMillis(),
            )
        }

        val event = DiscoveryEvent(
            id = "stamp_${regionId}_${System.currentTimeMillis()}",
            type = EntryType.STAMP_EARNED,
            title = "获得印章",
            description = "获得了 ${region.regionName} 的${rarity.displayName}",
            regionId = regionId,
        )
        _timeline.update { it + event }

        return PassportResult.StampEarned(regionId, rarity)
    }

    override suspend fun collectItem(collectibleId: String): PassportResult {
        val collectibles = _collectibles.value
        val collectible = collectibles.find { it.id == collectibleId }
            ?: return PassportResult.Error("Collectible not found")

        if (collectible.isCollected) {
            return PassportResult.Error("Already collected")
        }

        _collectibles.update { collectibleList ->
            collectibleList.map { c ->
                if (c.id == collectibleId) c.copy(
                    isCollected = true,
                    collectedAt = System.currentTimeMillis(),
                ) else c
            }
        }

        _passport.update { passport ->
            passport.copy(
                totalCollectibles = passport.totalCollectibles + 1,
                lastUpdated = System.currentTimeMillis(),
            )
        }

        val event = DiscoveryEvent(
            id = "collect_${collectibleId}_${System.currentTimeMillis()}",
            type = EntryType.COLLECTIBLE_FOUND,
            title = "发现收集品",
            description = "获得了 ${collectible.name} (${collectible.nameCn})",
            regionId = collectible.regionId,
        )
        _timeline.update { it + event }

        return PassportResult.CollectibleFound(collectibleId)
    }

    override suspend fun recordEntry(entry: PassportEntry): PassportResult {
        _entries.update { entries -> listOf(entry) + entries }
        _passport.update { passport ->
            passport.copy(
                lastUpdated = System.currentTimeMillis(),
            )
        }
        return PassportResult.Success("Entry recorded")
    }

    override suspend fun recordDiscovery(event: DiscoveryEvent): PassportResult {
        _timeline.update { it + event }
        return PassportResult.Success("Discovery recorded")
    }

    override suspend fun updateRegionProgress(regionId: String, progress: Float): PassportResult {
        _regions.update { regionList ->
            regionList.map { r ->
                if (r.regionId == regionId) r.copy(
                    completionPercentage = progress.coerceIn(0f, 1f),
                ) else r
            }
        }
        return PassportResult.Success("Progress updated")
    }

    override suspend fun addVocabularyLearned(regionId: String, count: Int): PassportResult {
        _regions.update { regionList ->
            regionList.map { r ->
                if (r.regionId == regionId) r.copy(
                    vocabularyLearned = r.vocabularyLearned + count,
                ) else r
            }
        }
        return PassportResult.Success("Vocabulary added")
    }

    override suspend fun addFriendshipMade(regionId: String): PassportResult {
        _regions.update { regionList ->
            regionList.map { r ->
                if (r.regionId == regionId) r.copy(
                    friendshipsMade = r.friendshipsMade + 1,
                ) else r
            }
        }
        return PassportResult.Success("Friendship recorded")
    }

    override suspend fun addQuestCompleted(regionId: String): PassportResult {
        _regions.update { regionList ->
            regionList.map { r ->
                if (r.regionId == regionId) r.copy(
                    questsCompleted = r.questsCompleted + 1,
                ) else r
            }
        }
        return PassportResult.Success("Quest recorded")
    }

    override suspend fun checkAchievements(): List<String> {
        val unlocked = mutableListOf<String>()
        val passport = _passport.value
        val collectibles = _collectibles.value
        val regions = _regions.value

        _achievements.update { achievementList ->
            achievementList.map { achievement ->
                when (achievement.id) {
                    "first_region" -> {
                        if (regions.any { it.isDiscovered } && !achievement.isUnlocked) {
                            unlocked.add(achievement.id)
                            achievement.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
                        } else achievement
                    }
                    "first_collectible" -> {
                        if (collectibles.any { it.isCollected } && !achievement.isUnlocked) {
                            unlocked.add(achievement.id)
                            achievement.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
                        } else achievement
                    }
                    "first_stamp" -> {
                        if (regions.any { it.stampEarned } && !achievement.isUnlocked) {
                            unlocked.add(achievement.id)
                            achievement.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
                        } else achievement
                    }
                    "all_regions_discovered" -> {
                        if (regions.all { it.isDiscovered } && !achievement.isUnlocked) {
                            unlocked.add(achievement.id)
                            achievement.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
                        } else achievement
                    }
                    "all_regions_completed" -> {
                        if (regions.all { it.isCompleted } && !achievement.isUnlocked) {
                            unlocked.add(achievement.id)
                            achievement.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
                        } else achievement
                    }
                    "collect_10" -> {
                        val count = collectibles.count { it.isCollected }
                        if (count >= 10 && !achievement.isUnlocked) {
                            unlocked.add(achievement.id)
                            achievement.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis(), currentCount = count)
                        } else achievement.copy(currentCount = count)
                    }
                    "collect_50" -> {
                        val count = collectibles.count { it.isCollected }
                        if (count >= 50 && !achievement.isUnlocked) {
                            unlocked.add(achievement.id)
                            achievement.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis(), currentCount = count)
                        } else achievement.copy(currentCount = count)
                    }
                    else -> achievement
                }
            }
        }

        return unlocked
    }

    override suspend fun getPassportStats(): PassportStats {
        val passport = _passport.value
        val regions = _regions.value
        val collectibles = _collectibles.value

        return PassportStats(
            totalRegions = regions.size,
            discoveredRegions = regions.count { it.isDiscovered },
            completedRegions = regions.count { it.isCompleted },
            totalStamps = regions.count { it.stampEarned },
            totalCollectibles = collectibles.size,
            collectedItems = collectibles.count { it.isCollected },
            totalDiscoveries = passport.totalDiscoveries,
            totalPlayTimeMinutes = regions.sumOf { it.totalPlayTimeMinutes },
            favoriteRegion = regions.maxByOrNull { it.totalPlayTimeMinutes }?.regionId,
            rarestCollectible = collectibles.filter { it.isCollected }.minByOrNull { it.rarity.dropChance }?.name,
            completionPercentage = passport.completionPercentage,
        )
    }

    
    private fun createInitialPassport() =
        PassportSeedData.createInitialPassport()

    
    private fun createInitialRegions() =
        PassportSeedData.createInitialRegions()

    
    private fun createInitialCollectibles() =
        PassportSeedData.createInitialCollectibles()

    
    private fun createInitialTimeline() =
        PassportSeedData.createInitialTimeline()

    
    private fun createInitialAchievements() =
        PassportSeedData.createInitialAchievements()
}
