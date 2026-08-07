package com.sworddao.phoenix.feature.passport.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PassportDao {

    @Query("SELECT * FROM passport LIMIT 1")
    fun getPassport(): Flow<PassportEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPassport(passport: PassportEntity)

    @Query("SELECT COUNT(*) FROM passport")
    suspend fun countPassport(): Int

    @Query("SELECT * FROM passport_region ORDER BY regionId")
    fun getAllRegions(): Flow<List<PassportRegionEntity>>

    @Query("SELECT * FROM passport_region WHERE regionId = :regionId")
    fun getPassportRegion(regionId: String): Flow<PassportRegionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegions(regions: List<PassportRegionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegion(region: PassportRegionEntity)

    @Query("SELECT COUNT(*) FROM passport_region")
    suspend fun countRegions(): Int

    @Query("SELECT * FROM passport_collectible ORDER BY id")
    fun getAllCollectibles(): Flow<List<CollectibleEntity>>

    @Query("SELECT * FROM passport_collectible WHERE regionId = :regionId ORDER BY id")
    fun getCollectiblesByRegion(regionId: String): Flow<List<CollectibleEntity>>

    @Query("SELECT * FROM passport_collectible WHERE category = :category ORDER BY id")
    fun getCollectiblesByCategory(category: String): Flow<List<CollectibleEntity>>

    @Query("SELECT * FROM passport_collectible WHERE id = :collectibleId")
    fun getCollectible(collectibleId: String): Flow<CollectibleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollectibles(collectibles: List<CollectibleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollectible(collectible: CollectibleEntity)

    @Query("SELECT COUNT(*) FROM passport_collectible")
    suspend fun countCollectibles(): Int

    @Query("SELECT * FROM passport_event ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<PassportEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<PassportEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(event: PassportEventEntity)

    @Query("SELECT COUNT(*) FROM passport_event")
    suspend fun countEvents(): Int

    @Query("SELECT * FROM passport_achievement ORDER BY id")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievements(achievements: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievement(achievement: AchievementEntity)

    @Query("SELECT COUNT(*) FROM passport_achievement")
    suspend fun countAchievements(): Int

    @Query("SELECT * FROM passport_entry ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<PassportEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: PassportEntryEntity)
}
