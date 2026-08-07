package com.sworddao.phoenix.feature.world.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldDao {

    @Query("SELECT * FROM world_region ORDER BY `order`")
    fun getAllRegions(): Flow<List<WorldRegionEntity>>

    @Query("SELECT * FROM world_region WHERE id = :regionId")
    fun getRegionById(regionId: String): Flow<WorldRegionEntity?>

    @Query("SELECT * FROM world_region WHERE status = :status ORDER BY `order`")
    fun getRegionsByStatus(status: String): Flow<List<WorldRegionEntity>>

    @Query("SELECT * FROM world_region WHERE status != 'LOCKED' ORDER BY `order`")
    fun getUnlockedRegions(): Flow<List<WorldRegionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegions(regions: List<WorldRegionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegion(region: WorldRegionEntity)

    @Query("SELECT COUNT(*) FROM world_region")
    suspend fun countRegions(): Int

    @Query("UPDATE world_region SET status = :status WHERE id = :regionId")
    suspend fun updateRegionStatus(regionId: String, status: String)

    @Query("SELECT * FROM world_region_progress")
    fun getAllProgress(): Flow<List<WorldRegionProgressEntity>>

    @Query("SELECT * FROM world_region_progress WHERE regionId = :regionId")
    fun getRegionProgress(regionId: String): Flow<WorldRegionProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: WorldRegionProgressEntity)

    @Query("SELECT COUNT(*) FROM world_region_progress")
    suspend fun countProgress(): Int

    @Query("SELECT * FROM world_connection")
    fun getAllConnections(): Flow<List<WorldConnectionEntity>>

    @Query("SELECT * FROM world_connection WHERE fromRegionId = :regionId OR toRegionId = :regionId")
    fun getConnectionsForRegion(regionId: String): Flow<List<WorldConnectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConnections(connections: List<WorldConnectionEntity>)

    @Query("SELECT COUNT(*) FROM world_connection")
    suspend fun countConnections(): Int

    @Query("SELECT * FROM world_location")
    fun getAllLocations(): Flow<List<WorldLocationEntity>>

    @Query("SELECT * FROM world_location WHERE regionId = :regionId ORDER BY id")
    fun getLocationsByRegion(regionId: String): Flow<List<WorldLocationEntity>>

    @Query("SELECT * FROM world_location WHERE id = :locationId")
    fun getLocationById(locationId: String): Flow<WorldLocationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocations(locations: List<WorldLocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(location: WorldLocationEntity)

    @Query("SELECT COUNT(*) FROM world_location")
    suspend fun countLocations(): Int

    @Query("UPDATE world_location SET isDiscovered = 1 WHERE id = :locationId")
    suspend fun markLocationDiscovered(locationId: String)

    @Query("SELECT * FROM world_landmark")
    fun getAllLandmarks(): Flow<List<WorldLandmarkEntity>>

    @Query("SELECT * FROM world_landmark WHERE regionId = :regionId ORDER BY id")
    fun getLandmarksByRegion(regionId: String): Flow<List<WorldLandmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLandmarks(landmarks: List<WorldLandmarkEntity>)

    @Query("SELECT COUNT(*) FROM world_landmark")
    suspend fun countLandmarks(): Int

    @Query("SELECT * FROM world_collectible")
    fun getAllCollectibles(): Flow<List<WorldCollectibleEntity>>

    @Query("SELECT * FROM world_collectible WHERE regionId = :regionId ORDER BY id")
    fun getCollectiblesByRegion(regionId: String): Flow<List<WorldCollectibleEntity>>

    @Query("SELECT * FROM world_collectible WHERE id = :collectibleId")
    fun getCollectibleById(collectibleId: String): Flow<WorldCollectibleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollectibles(collectibles: List<WorldCollectibleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollectible(collectible: WorldCollectibleEntity)

    @Query("SELECT COUNT(*) FROM world_collectible")
    suspend fun countCollectibles(): Int

    @Query("UPDATE world_collectible SET isCollected = 1 WHERE id = :collectibleId")
    suspend fun markCollectibleCollected(collectibleId: String)
}
