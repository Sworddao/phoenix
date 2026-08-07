package com.sworddao.phoenix.feature.world.data

import com.sworddao.phoenix.data.seed.WorldSeedData

import com.sworddao.phoenix.feature.world.domain.WorldRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockWorldRepository @Inject constructor() : WorldRepository {

    private val _regions = MutableStateFlow(createAllRegions())
    private val _progress = MutableStateFlow<Map<String, RegionProgress>>(emptyMap())
    private val _connections = MutableStateFlow(createConnections())
    private val _locations = MutableStateFlow(createLocations())
    private val _landmarks = MutableStateFlow(createLandmarks())
    private val _collectibles = MutableStateFlow(createCollectibles())

    override fun getAllRegions(): Flow<List<WorldRegion>> = _regions

    override fun getRegionById(regionId: String): Flow<WorldRegion?> =
        _regions.map { regions -> regions.find { it.id == regionId } }

    override fun getRegionConnections(regionId: String): Flow<List<RegionConnection>> =
        _connections.map { connections ->
            connections.filter { it.fromRegionId == regionId || it.toRegionId == regionId }
        }

    override fun getRegionProgress(regionId: String): Flow<RegionProgress?> =
        _progress.map { it[regionId] }

    override fun getExplorationProgress(): Flow<ExplorationProgress> = _regions.map { regions ->
        val progressMap = _progress.value
        val totalLocations = _locations.value.size
        val discoveredLocations = progressMap.values.sumOf { it.discoveredLocations.size }
        val totalCollectibles = _collectibles.value.size
        val collectedItems = progressMap.values.sumOf { it.collectedItems.size }

        ExplorationProgress(
            totalRegions = regions.size,
            completedRegions = regions.count { it.status == RegionStatus.COMPLETED },
            currentRegionId = regions.find { it.status == RegionStatus.CURRENT }?.id,
            totalLocations = totalLocations,
            discoveredLocations = discoveredLocations,
            totalCollectibles = totalCollectibles,
            collectedItems = collectedItems,
            completionPercentage = if (regions.isNotEmpty()) {
                regions.count { it.status == RegionStatus.COMPLETED }.toFloat() / regions.size
            } else 0f,
            regions = progressMap,
        )
    }

    override fun getCurrentRegion(): Flow<WorldRegion?> =
        _regions.map { regions -> regions.find { it.status == RegionStatus.CURRENT } }

    override fun getAvailableRegions(): Flow<List<WorldRegion>> =
        _regions.map { regions -> regions.filter { it.status == RegionStatus.AVAILABLE } }

    override fun getUnlockedRegions(): Flow<List<WorldRegion>> =
        _regions.map { regions -> regions.filter { it.isUnlocked } }

    override fun getLocationsByRegion(regionId: String): Flow<List<WorldLocation>> =
        _locations.map { locations -> locations.filter { it.regionId == regionId } }

    override fun getLandmarksByRegion(regionId: String): Flow<List<Landmark>> =
        _landmarks.map { landmarks -> landmarks.filter { it.regionId == regionId } }

    override fun getCollectiblesByRegion(regionId: String): Flow<List<CollectibleLocation>> =
        _collectibles.map { collectibles -> collectibles.filter { it.regionId == regionId } }

    override suspend fun travelToRegion(regionId: String): WorldResult {
        val regions = _regions.value
        val targetRegion = regions.find { it.id == regionId }
            ?: return WorldResult.Error("Region not found")

        if (targetRegion.status == RegionStatus.LOCKED) {
            return WorldResult.Error("Region is locked")
        }

        val currentRegion = regions.find { it.status == RegionStatus.CURRENT }

        _regions.update { regionList ->
            regionList.map { region ->
                when {
                    region.id == regionId -> region.copy(status = RegionStatus.CURRENT)
                    region.status == RegionStatus.CURRENT -> region.copy(status = RegionStatus.VISITED)
                    else -> region
                }
            }
        }

        val progress = _progress.value[regionId] ?: RegionProgress(
            regionId = regionId,
            status = RegionStatus.CURRENT,
        )
        _progress.update { it + (regionId to progress.copy(
            status = RegionStatus.CURRENT,
            lastVisitedAt = System.currentTimeMillis(),
            firstVisitedAt = progress.firstVisitedAt ?: System.currentTimeMillis(),
        )) }

        return WorldResult.TravelStarted(currentRegion?.id ?: "", regionId)
    }

    override suspend fun discoverLocation(locationId: String): WorldResult {
        val locations = _locations.value
        val location = locations.find { it.id == locationId }
            ?: return WorldResult.Error("Location not found")

        _locations.update { locationList ->
            locationList.map { loc ->
                if (loc.id == locationId) loc.copy(isDiscovered = true) else loc
            }
        }

        val regionId = location.regionId
        val progress = _progress.value[regionId] ?: RegionProgress(
            regionId = regionId,
            status = RegionStatus.CURRENT,
        )
        _progress.update { it + (regionId to progress.copy(
            discoveredLocations = progress.discoveredLocations + locationId,
        )) }

        return WorldResult.LocationDiscovered(locationId)
    }

    override suspend fun collectItem(collectibleId: String): WorldResult {
        val collectibles = _collectibles.value
        val collectible = collectibles.find { it.id == collectibleId }
            ?: return WorldResult.Error("Collectible not found")

        if (collectible.isCollected) {
            return WorldResult.Error("Already collected")
        }

        _collectibles.update { collectibleList ->
            collectibleList.map { c ->
                if (c.id == collectibleId) c.copy(isCollected = true) else c
            }
        }

        val regionId = collectible.regionId
        val progress = _progress.value[regionId] ?: RegionProgress(
            regionId = regionId,
            status = RegionStatus.CURRENT,
        )
        _progress.update { it + (regionId to progress.copy(
            collectedItems = progress.collectedItems + collectibleId,
        )) }

        return WorldResult.CollectibleFound(collectibleId)
    }

    override suspend fun checkRegionUnlocks(): List<String> {
        val regions = _regions.value
        val newlyUnlocked = mutableListOf<String>()

        regions.forEach { region ->
            if (region.status == RegionStatus.LOCKED) {
                val requirements = region.unlockRequirements
                val allMet = checkRequirements(requirements, regions)
                if (allMet) {
                    newlyUnlocked.add(region.id)
                }
            }
        }

        if (newlyUnlocked.isNotEmpty()) {
            _regions.update { regionList ->
                regionList.map { region ->
                    if (newlyUnlocked.contains(region.id)) {
                        region.copy(status = RegionStatus.AVAILABLE)
                    } else region
                }
            }
        }

        return newlyUnlocked
    }

    override suspend fun completeRegion(regionId: String): WorldResult {
        val regions = _regions.value
        val region = regions.find { it.id == regionId }
            ?: return WorldResult.Error("Region not found")

        _regions.update { regionList ->
            regionList.map { r ->
                if (r.id == regionId) r.copy(
                    status = RegionStatus.COMPLETED,
                    completionPercentage = 100f,
                ) else r
            }
        }

        val progress = _progress.value[regionId] ?: RegionProgress(
            regionId = regionId,
            status = RegionStatus.COMPLETED,
        )
        _progress.update { it + (regionId to progress.copy(
            status = RegionStatus.COMPLETED,
            completionPercentage = 100f,
        )) }

        // Check for newly unlocked regions
        checkRegionUnlocks()

        return WorldResult.RegionUnlocked(regionId)
    }

    override suspend fun unlockFastTravel(regionId: String): WorldResult {
        val progress = _progress.value[regionId] ?: RegionProgress(
            regionId = regionId,
            status = RegionStatus.VISITED,
        )
        _progress.update { it + (regionId to progress.copy(unlockedFastTravel = true)) }

        return WorldResult.Success("Fast travel unlocked for $regionId")
    }

    override suspend fun getRegionNpcs(regionId: String): Flow<List<String>> =
        _regions.map { regions ->
            regions.find { it.id == regionId }?.npcIds ?: emptyList()
        }

    override suspend fun getRegionQuests(regionId: String): Flow<List<String>> =
        _regions.map { regions ->
            regions.find { it.id == regionId }?.questIds ?: emptyList()
        }

    private fun checkRequirements(
        requirements: UnlockRequirement,
        regions: List<WorldRegion>,
    ): Boolean {
        if (requirements.questIds.isNotEmpty()) {
            val completedQuests = _progress.value.values.flatMap { it.completedQuests }
            if (!requirements.questIds.all { it in completedQuests }) return false
        }

        if (requirements.requiredRegions.isNotEmpty()) {
            val completedRegions = regions.filter { it.status == RegionStatus.COMPLETED }.map { it.id }
            if (!requirements.requiredRegions.all { it in completedRegions }) return false
        }

        return true
    }

    
    private fun createAllRegions(): List<WorldRegion> =
        WorldSeedData.createAllRegions()

    
    private fun createConnections(): List<RegionConnection> =
        WorldSeedData.createConnections()

    
    private fun createLocations(): List<WorldLocation> =
        WorldSeedData.createLocations()

    
    private fun createLandmarks(): List<Landmark> =
        WorldSeedData.createLandmarks()

    
    private fun createCollectibles(): List<CollectibleLocation> =
        WorldSeedData.createCollectibles()
}
