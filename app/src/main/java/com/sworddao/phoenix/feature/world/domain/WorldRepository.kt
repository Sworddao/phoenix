package com.sworddao.phoenix.feature.world.domain

import com.sworddao.phoenix.feature.world.data.CollectibleLocation
import com.sworddao.phoenix.feature.world.data.ExplorationProgress
import com.sworddao.phoenix.feature.world.data.Landmark
import com.sworddao.phoenix.feature.world.data.RegionConnection
import com.sworddao.phoenix.feature.world.data.RegionProgress
import com.sworddao.phoenix.feature.world.data.WorldLocation
import com.sworddao.phoenix.feature.world.data.WorldRegion
import com.sworddao.phoenix.feature.world.data.WorldResult
import kotlinx.coroutines.flow.Flow

interface WorldRepository {
    fun getAllRegions(): Flow<List<WorldRegion>>
    fun getRegionById(regionId: String): Flow<WorldRegion?>
    fun getRegionConnections(regionId: String): Flow<List<RegionConnection>>
    fun getRegionProgress(regionId: String): Flow<RegionProgress?>
    fun getExplorationProgress(): Flow<ExplorationProgress>
    fun getCurrentRegion(): Flow<WorldRegion?>
    fun getAvailableRegions(): Flow<List<WorldRegion>>
    fun getUnlockedRegions(): Flow<List<WorldRegion>>
    fun getLocationsByRegion(regionId: String): Flow<List<WorldLocation>>
    fun getLandmarksByRegion(regionId: String): Flow<List<Landmark>>
    fun getCollectiblesByRegion(regionId: String): Flow<List<CollectibleLocation>>

    suspend fun travelToRegion(regionId: String): WorldResult
    suspend fun discoverLocation(locationId: String): WorldResult
    suspend fun collectItem(collectibleId: String): WorldResult
    suspend fun checkRegionUnlocks(): List<String>
    suspend fun completeRegion(regionId: String): WorldResult
    suspend fun unlockFastTravel(regionId: String): WorldResult
    suspend fun getRegionNpcs(regionId: String): Flow<List<String>>
    suspend fun getRegionQuests(regionId: String): Flow<List<String>>
}
