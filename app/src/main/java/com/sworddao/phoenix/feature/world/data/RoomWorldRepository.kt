package com.sworddao.phoenix.feature.world.data

import com.sworddao.phoenix.data.seed.WorldSeedData
import com.sworddao.phoenix.feature.world.domain.WorldRepository
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
class RoomWorldRepository @Inject constructor(
    private val dao: WorldDao,
) : WorldRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.countRegions() == 0) {
                dao.upsertRegions(WorldSeedData.createAllRegions().map { it.toEntity() })
            }
            if (dao.countConnections() == 0) {
                dao.upsertConnections(WorldSeedData.createConnections().map { it.toEntity() })
            }
            if (dao.countLocations() == 0) {
                dao.upsertLocations(WorldSeedData.createLocations().map { it.toEntity() })
            }
            if (dao.countLandmarks() == 0) {
                dao.upsertLandmarks(WorldSeedData.createLandmarks().map { it.toEntity() })
            }
            if (dao.countCollectibles() == 0) {
                dao.upsertCollectibles(WorldSeedData.createCollectibles().map { it.toEntity() })
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    private fun regionFlow(): Flow<List<WorldRegion>> =
        combine(dao.getAllRegions(), dao.getAllLandmarks(), dao.getAllCollectibles()) { regions, landmarks, collectibles ->
            regions.map { region ->
                region.toDomain(
                    landmarks = landmarks.map { it.toDomain() }.filter { it.regionId == region.id },
                    collectibles = collectibles.map { it.toDomain() }.filter { it.regionId == region.id },
                )
            }
        }

    override fun getAllRegions(): Flow<List<WorldRegion>> = seededFlow { regionFlow() }

    override fun getRegionById(regionId: String): Flow<WorldRegion?> = seededFlow {
        combine(dao.getRegionById(regionId), dao.getAllLandmarks(), dao.getAllCollectibles()) { region, landmarks, collectibles ->
            region?.toDomain(
                landmarks = landmarks.map { it.toDomain() }.filter { it.regionId == regionId },
                collectibles = collectibles.map { it.toDomain() }.filter { it.regionId == regionId },
            )
        }
    }

    override fun getRegionConnections(regionId: String): Flow<List<RegionConnection>> =
        seededFlow { dao.getConnectionsForRegion(regionId).map { list -> list.map { it.toDomain() } } }

    override fun getRegionProgress(regionId: String): Flow<RegionProgress?> =
        seededFlow { dao.getRegionProgress(regionId).map { it?.toDomain() } }

    override fun getExplorationProgress(): Flow<ExplorationProgress> = seededFlow {
        combine(regionFlow(), dao.getAllProgress(), dao.getAllLocations(), dao.getAllCollectibles()) { regions, progressList, locations, collectibles ->
            val progressMap = progressList.associate { it.regionId to it.toDomain() }
            ExplorationProgress(
                totalRegions = regions.size,
                completedRegions = regions.count { it.status == RegionStatus.COMPLETED },
                currentRegionId = regions.find { it.status == RegionStatus.CURRENT }?.id,
                totalLocations = locations.size,
                discoveredLocations = progressMap.values.sumOf { it.discoveredLocations.size },
                totalCollectibles = collectibles.size,
                collectedItems = progressMap.values.sumOf { it.collectedItems.size },
                completionPercentage = if (regions.isNotEmpty()) {
                    regions.count { it.status == RegionStatus.COMPLETED }.toFloat() / regions.size
                } else 0f,
                regions = progressMap,
            )
        }
    }

    override fun getCurrentRegion(): Flow<WorldRegion?> =
        seededFlow { regionFlow().map { regions -> regions.find { it.status == RegionStatus.CURRENT } } }

    override fun getAvailableRegions(): Flow<List<WorldRegion>> =
        seededFlow { regionFlow().map { regions -> regions.filter { it.status == RegionStatus.AVAILABLE } } }

    override fun getUnlockedRegions(): Flow<List<WorldRegion>> =
        seededFlow { regionFlow().map { regions -> regions.filter { it.isUnlocked } } }

    override fun getLocationsByRegion(regionId: String): Flow<List<WorldLocation>> =
        seededFlow { dao.getLocationsByRegion(regionId).map { list -> list.map { it.toDomain() } } }

    override fun getLandmarksByRegion(regionId: String): Flow<List<Landmark>> =
        seededFlow { dao.getLandmarksByRegion(regionId).map { list -> list.map { it.toDomain() } } }

    override fun getCollectiblesByRegion(regionId: String): Flow<List<CollectibleLocation>> =
        seededFlow { dao.getCollectiblesByRegion(regionId).map { list -> list.map { it.toDomain() } } }

    override suspend fun travelToRegion(regionId: String): WorldResult {
        ensureSeeded()
        val targetRegion = dao.getRegionById(regionId).first()?.toDomain()
            ?: return WorldResult.Error("Region not found")
        if (targetRegion.status == RegionStatus.LOCKED) {
            return WorldResult.Error("Region is locked")
        }
        val currentRegion = dao.getAllRegions().first()
            .firstOrNull { it.status == RegionStatus.CURRENT.name }?.toDomain()

        val allRegions = dao.getAllRegions().first()
        allRegions.forEach { region ->
            when {
                region.id == regionId -> dao.updateRegionStatus(regionId, RegionStatus.CURRENT.name)
                region.status == RegionStatus.CURRENT.name ->
                    dao.updateRegionStatus(region.id, RegionStatus.VISITED.name)
            }
        }

        val now = System.currentTimeMillis()
        val existing = dao.getRegionProgress(regionId).first()?.toDomain()
            ?: RegionProgress(regionId = regionId, status = RegionStatus.CURRENT)
        dao.upsertProgress(
            existing.copy(
                status = RegionStatus.CURRENT,
                lastVisitedAt = now,
                firstVisitedAt = existing.firstVisitedAt ?: now,
            ).toEntity()
        )

        return WorldResult.TravelStarted(currentRegion?.id ?: "", regionId)
    }

    override suspend fun discoverLocation(locationId: String): WorldResult {
        ensureSeeded()
        val location = dao.getLocationById(locationId).first()?.toDomain()
            ?: return WorldResult.Error("Location not found")
        dao.markLocationDiscovered(locationId)

        val regionId = location.regionId
        val existing = dao.getRegionProgress(regionId).first()?.toDomain()
            ?: RegionProgress(regionId = regionId, status = RegionStatus.CURRENT)
        dao.upsertProgress(
            existing.copy(discoveredLocations = existing.discoveredLocations + locationId).toEntity()
        )
        return WorldResult.LocationDiscovered(locationId)
    }

    override suspend fun collectItem(collectibleId: String): WorldResult {
        ensureSeeded()
        val collectible = dao.getCollectibleById(collectibleId).first()?.toDomain()
            ?: return WorldResult.Error("Collectible not found")
        if (collectible.isCollected) {
            return WorldResult.Error("Already collected")
        }
        dao.markCollectibleCollected(collectibleId)

        val regionId = collectible.regionId
        val existing = dao.getRegionProgress(regionId).first()?.toDomain()
            ?: RegionProgress(regionId = regionId, status = RegionStatus.CURRENT)
        dao.upsertProgress(
            existing.copy(collectedItems = existing.collectedItems + collectibleId).toEntity()
        )
        return WorldResult.CollectibleFound(collectibleId)
    }

    override suspend fun checkRegionUnlocks(): List<String> {
        ensureSeeded()
        val regionEntities = dao.getAllRegions().first()
        val regions = regionEntities.map { it.toDomain() }
        val progressMap = dao.getAllProgress().first()
            .associate { it.regionId to it.toDomain() }

        val newlyUnlocked = regions.filter { region ->
            region.status == RegionStatus.LOCKED && checkRequirements(region.unlockRequirements, regions, progressMap)
        }.map { it.id }

        newlyUnlocked.forEach { dao.updateRegionStatus(it, RegionStatus.AVAILABLE.name) }
        return newlyUnlocked
    }

    override suspend fun completeRegion(regionId: String): WorldResult {
        ensureSeeded()
        val region = dao.getRegionById(regionId).first()?.toDomain()
            ?: return WorldResult.Error("Region not found")

        dao.updateRegionStatus(regionId, RegionStatus.COMPLETED.name)
        val existing = dao.getRegionProgress(regionId).first()?.toDomain()
            ?: RegionProgress(regionId = regionId, status = RegionStatus.COMPLETED)
        dao.upsertProgress(
            existing.copy(status = RegionStatus.COMPLETED, completionPercentage = 100f).toEntity()
        )
        dao.upsertRegion(region.copy(status = RegionStatus.COMPLETED, completionPercentage = 100f).toEntity())

        checkRegionUnlocks()
        return WorldResult.RegionUnlocked(regionId)
    }

    override suspend fun unlockFastTravel(regionId: String): WorldResult {
        ensureSeeded()
        val existing = dao.getRegionProgress(regionId).first()?.toDomain()
            ?: RegionProgress(regionId = regionId, status = RegionStatus.VISITED)
        dao.upsertProgress(existing.copy(unlockedFastTravel = true).toEntity())
        return WorldResult.Success("Fast travel unlocked for $regionId")
    }

    override suspend fun getRegionNpcs(regionId: String): Flow<List<String>> =
        seededFlow { dao.getRegionById(regionId).map { it?.npcIdsJson?.let { json -> com.sworddao.phoenix.data.local.RoomJson.fromJsonList<String>(json) } ?: emptyList() } }

    override suspend fun getRegionQuests(regionId: String): Flow<List<String>> =
        seededFlow { dao.getRegionById(regionId).map { it?.questIdsJson?.let { json -> com.sworddao.phoenix.data.local.RoomJson.fromJsonList<String>(json) } ?: emptyList() } }

    private fun checkRequirements(
        requirements: UnlockRequirement,
        regions: List<WorldRegion>,
        progressMap: Map<String, RegionProgress>,
    ): Boolean {
        if (requirements.questIds.isNotEmpty()) {
            val completedQuests = progressMap.values.flatMap { it.completedQuests }
            if (!requirements.questIds.all { it in completedQuests }) return false
        }
        if (requirements.requiredRegions.isNotEmpty()) {
            val completedRegions = regions.filter { it.status == RegionStatus.COMPLETED }.map { it.id }
            if (!requirements.requiredRegions.all { it in completedRegions }) return false
        }
        return true
    }
}
