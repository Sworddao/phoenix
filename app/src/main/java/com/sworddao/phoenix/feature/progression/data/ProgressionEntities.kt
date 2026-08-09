package com.sworddao.phoenix.feature.progression.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class SourceSnapshot(
    val dialogues: Int = 0,
    val wordsDiscovered: Int = 0,
    val questsCompleted: Int = 0,
    val friendshipLevels: Int = 0,
    val passportStamps: Int = 0,
    val speakingPractices: Int = 0,
    val listeningPractices: Int = 0,
    val readingPractices: Int = 0,
    val writingPractices: Int = 0,
    val regionsUnlocked: Int = 0,
    val regionsCompleted: Int = 0,
    val achievements: Int = 0,
)

@Entity(tableName = "progression_state")
data class ProgressionStateEntity(
    @PrimaryKey val id: String,
    val lastTotalXp: Int,
    val lastLevel: Int,
    val dailyDate: String,
    val goalStreak: Int,
    val lastUnlockedChaptersJson: String,
)

@Entity(tableName = "progression_snapshot")
data class ProgressionSnapshotEntity(
    @PrimaryKey val id: String,
    val snapshotJson: String?,
)

@Entity(tableName = "progression_daily")
data class ProgressionDailyEntity(
    @PrimaryKey val id: String,
    val dailyJson: String,
)

@Entity(tableName = "progression_recent")
data class ProgressionRecentEntity(
    @PrimaryKey val id: String,
    val recentJson: String,
)

@Entity(tableName = "progression_features")
data class ProgressionFeaturesEntity(
    @PrimaryKey val id: String,
    val timelineJson: String,
)

@Entity(tableName = "progression_player")
data class ProgressionPlayerEntity(
    @PrimaryKey val id: String,
    val playerJson: String,
)

@Entity(tableName = "progression_learning")
data class ProgressionLearningEntity(
    @PrimaryKey val id: String,
    val learningJson: String,
)

@Entity(tableName = "progression_objectives")
data class ProgressionObjectivesEntity(
    @PrimaryKey val id: String,
    val objectivesJson: String,
)

@Dao
interface ProgressionDao {

    @Query("SELECT * FROM progression_state WHERE id = 'all'")
    fun getStateDoc(): Flow<ProgressionStateEntity?>

    @Query("SELECT * FROM progression_state WHERE id = 'all'")
    suspend fun getStateDocOnce(): ProgressionStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStateDoc(entity: ProgressionStateEntity)

    @Query("SELECT * FROM progression_snapshot WHERE id = 'all'")
    fun getSnapshotDoc(): Flow<ProgressionSnapshotEntity?>

    @Query("SELECT * FROM progression_snapshot WHERE id = 'all'")
    suspend fun getSnapshotDocOnce(): ProgressionSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshotDoc(entity: ProgressionSnapshotEntity)

    @Query("SELECT * FROM progression_daily WHERE id = 'all'")
    fun getDailyDoc(): Flow<ProgressionDailyEntity?>

    @Query("SELECT * FROM progression_daily WHERE id = 'all'")
    suspend fun getDailyDocOnce(): ProgressionDailyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyDoc(entity: ProgressionDailyEntity)

    @Query("SELECT * FROM progression_recent WHERE id = 'all'")
    fun getRecentDoc(): Flow<ProgressionRecentEntity?>

    @Query("SELECT * FROM progression_recent WHERE id = 'all'")
    suspend fun getRecentDocOnce(): ProgressionRecentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentDoc(entity: ProgressionRecentEntity)

    @Query("SELECT * FROM progression_features WHERE id = 'all'")
    fun getFeaturesDoc(): Flow<ProgressionFeaturesEntity?>

    @Query("SELECT * FROM progression_features WHERE id = 'all'")
    suspend fun getFeaturesDocOnce(): ProgressionFeaturesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeaturesDoc(entity: ProgressionFeaturesEntity)

    @Query("SELECT * FROM progression_player WHERE id = 'all'")
    fun getPlayerDoc(): Flow<ProgressionPlayerEntity?>

    @Query("SELECT * FROM progression_player WHERE id = 'all'")
    suspend fun getPlayerDocOnce(): ProgressionPlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerDoc(entity: ProgressionPlayerEntity)

    @Query("SELECT * FROM progression_learning WHERE id = 'all'")
    fun getLearningDoc(): Flow<ProgressionLearningEntity?>

    @Query("SELECT * FROM progression_learning WHERE id = 'all'")
    suspend fun getLearningDocOnce(): ProgressionLearningEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearningDoc(entity: ProgressionLearningEntity)

    @Query("SELECT * FROM progression_objectives WHERE id = 'all'")
    fun getObjectivesDoc(): Flow<ProgressionObjectivesEntity?>

    @Query("SELECT * FROM progression_objectives WHERE id = 'all'")
    suspend fun getObjectivesDocOnce(): ProgressionObjectivesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertObjectivesDoc(entity: ProgressionObjectivesEntity)
}
