package com.sworddao.phoenix.feature.passport.data

import com.sworddao.phoenix.data.seed.PassportSeedData
import com.sworddao.phoenix.feature.passport.domain.PassportRepository
import com.sworddao.phoenix.feature.passport.domain.PassportStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPassportRepository @Inject constructor(
    private val dao: PassportDao,
) : PassportRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.countPassport() == 0) {
                dao.upsertPassport(PassportSeedData.createInitialPassport().toEntity())
            }
            if (dao.countRegions() == 0) {
                dao.upsertRegions(PassportSeedData.createInitialRegions().map { it.toEntity() })
            }
            if (dao.countCollectibles() == 0) {
                dao.upsertCollectibles(PassportSeedData.createInitialCollectibles().map { it.toEntity() })
            }
            if (dao.countEvents() == 0) {
                dao.upsertEvents(PassportSeedData.createInitialTimeline().map { it.toEntity() })
            }
            if (dao.countAchievements() == 0) {
                dao.upsertAchievements(PassportSeedData.createInitialAchievements().map { it.toEntity() })
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    override fun getPassport(): Flow<Passport> = seededFlow {
        combine(
            dao.getPassport(),
            dao.getAllRegions(),
            dao.getAllCollectibles(),
            dao.getAllEvents(),
        ) { passport, regions, collectibles, events ->
            val base = passport?.toDomain() ?: Passport()
            base.copy(
                regions = regions.associate { it.regionId to it.toDomain() },
                collectibles = collectibles.associate { it.id to it.toDomain() },
                timeline = events.map { it.toDomain() },
            )
        }
    }

    override fun getPassportRegion(regionId: String): Flow<PassportRegion?> =
        seededFlow { dao.getPassportRegion(regionId).map { it?.toDomain() } }

    override fun getAllRegions(): Flow<List<PassportRegion>> =
        seededFlow { dao.getAllRegions().map { list -> list.map { it.toDomain() } } }

    override fun getCollectibles(): Flow<List<Collectible>> =
        seededFlow { dao.getAllCollectibles().map { list -> list.map { it.toDomain() } } }

    override fun getCollectiblesByRegion(regionId: String): Flow<List<Collectible>> =
        seededFlow { dao.getCollectiblesByRegion(regionId).map { list -> list.map { it.toDomain() } } }

    override fun getCollectiblesByCategory(category: CollectibleCategory): Flow<List<Collectible>> =
        seededFlow { dao.getCollectiblesByCategory(category.name).map { list -> list.map { it.toDomain() } } }

    override fun getCollectionProgress(): Flow<CollectionProgress> = seededFlow {
        dao.getAllCollectibles().map { entities ->
            val collectibles = entities.map { it.toDomain() }
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
    }

    override fun getDiscoveryTimeline(): Flow<List<DiscoveryEvent>> =
        seededFlow { dao.getAllEvents().map { list -> list.map { it.toDomain() } } }

    override fun getAchievements(): Flow<List<AchievementProgress>> =
        seededFlow { dao.getAllAchievements().map { list -> list.map { it.toDomain() } } }

    override fun getRecentEntries(limit: Int): Flow<List<PassportEntry>> = seededFlow {
        combine(dao.getPassport(), dao.getAllRegions(), dao.getAllEntries()) { passport, regions, entries ->
            val regionEntries = regions.map { it.toDomain() }
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
            (entries.map { it.toDomain() } + regionEntries)
                .sortedByDescending { it.timestamp }
                .take(limit)
        }
    }

    override suspend fun discoverRegion(regionId: String): PassportResult {
        ensureSeeded()
        val region = dao.getPassportRegion(regionId).first()?.toDomain()
            ?: return PassportResult.Error("Region not found")
        if (region.isDiscovered) {
            return PassportResult.Error("Region already discovered")
        }
        val now = System.currentTimeMillis()
        dao.upsertRegion(region.copy(isDiscovered = true, discoveredAt = now).toEntity())

        dao.upsertEvent(
            DiscoveryEvent(
                id = "discovery_${regionId}_$now",
                type = EntryType.REGION_DISCOVERED,
                title = "发现新区域",
                description = "发现了 ${region.regionName} (${region.regionNameCn})",
                regionId = regionId,
            ).toEntity()
        )

        val passport = dao.getPassport().first()?.toDomain() ?: Passport()
        dao.upsertPassport(
            passport.copy(totalDiscoveries = passport.totalDiscoveries + 1, lastUpdated = now).toEntity()
        )

        return PassportResult.Success("Region discovered: ${region.regionName}")
    }

    override suspend fun completeRegion(regionId: String): PassportResult {
        ensureSeeded()
        val region = dao.getPassportRegion(regionId).first()?.toDomain()
            ?: return PassportResult.Error("Region not found")
        if (!region.isDiscovered) {
            return PassportResult.Error("Region not discovered yet")
        }
        if (region.isCompleted) {
            return PassportResult.Error("Region already completed")
        }
        val now = System.currentTimeMillis()
        dao.upsertRegion(
            region.copy(isCompleted = true, completedAt = now, completionPercentage = 100f).toEntity()
        )
        dao.upsertEvent(
            DiscoveryEvent(
                id = "completion_${regionId}_$now",
                type = EntryType.REGION_COMPLETED,
                title = "区域完成",
                description = "完成了 ${region.regionName} 的探索",
                regionId = regionId,
            ).toEntity()
        )
        return PassportResult.RegionCompleted(regionId)
    }

    override suspend fun earnStamp(regionId: String): PassportResult {
        ensureSeeded()
        val region = dao.getPassportRegion(regionId).first()?.toDomain()
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
        val now = System.currentTimeMillis()
        dao.upsertRegion(region.copy(stampEarned = true, stampRarity = rarity).toEntity())

        val passport = dao.getPassport().first()?.toDomain() ?: Passport()
        dao.upsertPassport(
            passport.copy(totalStamps = passport.totalStamps + 1, lastUpdated = now).toEntity()
        )
        dao.upsertEvent(
            DiscoveryEvent(
                id = "stamp_${regionId}_$now",
                type = EntryType.STAMP_EARNED,
                title = "获得印章",
                description = "获得了 ${region.regionName} 的${rarity.displayName}",
                regionId = regionId,
            ).toEntity()
        )
        return PassportResult.StampEarned(regionId, rarity)
    }

    override suspend fun collectItem(collectibleId: String): PassportResult {
        ensureSeeded()
        val collectible = dao.getCollectible(collectibleId).first()?.toDomain()
            ?: return PassportResult.Error("Collectible not found")
        if (collectible.isCollected) {
            return PassportResult.Error("Already collected")
        }
        val now = System.currentTimeMillis()
        dao.upsertCollectible(
            collectible.copy(isCollected = true, collectedAt = now).toEntity()
        )

        val passport = dao.getPassport().first()?.toDomain() ?: Passport()
        dao.upsertPassport(
            passport.copy(totalCollectibles = passport.totalCollectibles + 1, lastUpdated = now).toEntity()
        )
        dao.upsertEvent(
            DiscoveryEvent(
                id = "collect_${collectibleId}_$now",
                type = EntryType.COLLECTIBLE_FOUND,
                title = "发现收集品",
                description = "获得了 ${collectible.name} (${collectible.nameCn})",
                regionId = collectible.regionId,
            ).toEntity()
        )
        return PassportResult.CollectibleFound(collectibleId)
    }

    override suspend fun recordEntry(entry: PassportEntry): PassportResult {
        ensureSeeded()
        dao.upsertEntry(entry.toEntity())
        val passport = dao.getPassport().first()?.toDomain() ?: Passport()
        dao.upsertPassport(passport.copy(lastUpdated = System.currentTimeMillis()).toEntity())
        return PassportResult.Success("Entry recorded")
    }

    override suspend fun recordDiscovery(event: DiscoveryEvent): PassportResult {
        ensureSeeded()
        dao.upsertEvent(event.toEntity())
        return PassportResult.Success("Discovery recorded")
    }

    override suspend fun updateRegionProgress(regionId: String, progress: Float): PassportResult {
        ensureSeeded()
        val region = dao.getPassportRegion(regionId).first()?.toDomain()
            ?: return PassportResult.Error("Region not found")
        dao.upsertRegion(region.copy(completionPercentage = progress.coerceIn(0f, 1f)).toEntity())
        return PassportResult.Success("Progress updated")
    }

    override suspend fun addVocabularyLearned(regionId: String, count: Int): PassportResult {
        ensureSeeded()
        val region = dao.getPassportRegion(regionId).first()?.toDomain()
            ?: return PassportResult.Error("Region not found")
        dao.upsertRegion(region.copy(vocabularyLearned = region.vocabularyLearned + count).toEntity())
        return PassportResult.Success("Vocabulary added")
    }

    override suspend fun addFriendshipMade(regionId: String): PassportResult {
        ensureSeeded()
        val region = dao.getPassportRegion(regionId).first()?.toDomain()
            ?: return PassportResult.Error("Region not found")
        dao.upsertRegion(region.copy(friendshipsMade = region.friendshipsMade + 1).toEntity())
        return PassportResult.Success("Friendship recorded")
    }

    override suspend fun addQuestCompleted(regionId: String): PassportResult {
        ensureSeeded()
        val region = dao.getPassportRegion(regionId).first()?.toDomain()
            ?: return PassportResult.Error("Region not found")
        dao.upsertRegion(region.copy(questsCompleted = region.questsCompleted + 1).toEntity())
        return PassportResult.Success("Quest recorded")
    }

    override suspend fun checkAchievements(): List<String> {
        ensureSeeded()
        val unlocked = mutableListOf<String>()
        val passport = dao.getPassport().first()?.toDomain() ?: Passport()
        val collectibles = dao.getAllCollectibles().first().map { it.toDomain() }
        val regions = dao.getAllRegions().first().map { it.toDomain() }

        dao.getAllAchievements().first().map { it.toDomain() }.forEach { achievement ->
            val updated = when (achievement.id) {
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
            dao.upsertAchievement(updated.toEntity())
        }

        return unlocked
    }

    override suspend fun getPassportStats(): PassportStats {
        ensureSeeded()
        val passport = dao.getPassport().first()?.toDomain() ?: Passport()
        val regions = dao.getAllRegions().first().map { it.toDomain() }
        val collectibles = dao.getAllCollectibles().first().map { it.toDomain() }
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
}
