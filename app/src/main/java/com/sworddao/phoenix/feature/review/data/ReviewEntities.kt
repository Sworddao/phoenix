package com.sworddao.phoenix.feature.review.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class ReviewSourceSnapshot(
    val wordsDiscovered: Int = 0,
    val dialogues: Int = 0,
    val questsCompleted: Int = 0,
    val friendshipLevels: Int = 0,
    val passportStamps: Int = 0,
    val speakingPractices: Int = 0,
    val listeningPractices: Int = 0,
    val readingPractices: Int = 0,
    val writingPractices: Int = 0,
    val regionsUnlocked: Int = 0,
)

@Entity(tableName = "review_items")
data class ReviewItemsEntity(
    @PrimaryKey val id: String,
    val itemsJson: String,
)

@Entity(tableName = "review_memory")
data class ReviewMemoryEntity(
    @PrimaryKey val id: String,
    val memoryJson: String,
)

@Entity(tableName = "review_schedules")
data class ReviewSchedulesEntity(
    @PrimaryKey val id: String,
    val schedulesJson: String,
)

@Entity(tableName = "review_sessions")
data class ReviewSessionsEntity(
    @PrimaryKey val id: String,
    val sessionsJson: String,
)

@Entity(tableName = "review_history")
data class ReviewHistoryEntity(
    @PrimaryKey val id: String,
    val historyJson: String,
)

@Entity(tableName = "review_state")
data class ReviewStateEntity(
    @PrimaryKey val id: String,
    val itemIdCounter: Int,
    val historyIdCounter: Int,
    val sessionIdCounter: Int,
    val todayDate: String,
    val reviewsToday: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val xpEarnedTotal: Int,
    val reviewedWordIdsJson: String,
)

@Entity(tableName = "review_snapshot")
data class ReviewSnapshotEntity(
    @PrimaryKey val id: String,
    val snapshotJson: String?,
)

@Entity(tableName = "review_stats")
data class ReviewStatsEntity(
    @PrimaryKey val id: String,
    val statsJson: String,
)

@Entity(tableName = "review_published")
data class ReviewPublishedEntity(
    @PrimaryKey val id: String,
    val todayJson: String,
    val upcomingJson: String,
    val recommendationsJson: String,
    val dailyJson: String,
    val memoryStrengthsJson: String,
)

@Dao
interface ReviewDao {

    @Query("SELECT * FROM review_items WHERE id = 'all'")
    fun getItemsDoc(): Flow<ReviewItemsEntity?>

    @Query("SELECT * FROM review_items WHERE id = 'all'")
    suspend fun getItemsDocOnce(): ReviewItemsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItemsDoc(entity: ReviewItemsEntity)

    @Query("SELECT * FROM review_memory WHERE id = 'all'")
    fun getMemoryDoc(): Flow<ReviewMemoryEntity?>

    @Query("SELECT * FROM review_memory WHERE id = 'all'")
    suspend fun getMemoryDocOnce(): ReviewMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemoryDoc(entity: ReviewMemoryEntity)

    @Query("SELECT * FROM review_schedules WHERE id = 'all'")
    fun getSchedulesDoc(): Flow<ReviewSchedulesEntity?>

    @Query("SELECT * FROM review_schedules WHERE id = 'all'")
    suspend fun getSchedulesDocOnce(): ReviewSchedulesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedulesDoc(entity: ReviewSchedulesEntity)

    @Query("SELECT * FROM review_sessions WHERE id = 'all'")
    fun getSessionsDoc(): Flow<ReviewSessionsEntity?>

    @Query("SELECT * FROM review_sessions WHERE id = 'all'")
    suspend fun getSessionsDocOnce(): ReviewSessionsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionsDoc(entity: ReviewSessionsEntity)

    @Query("SELECT * FROM review_history WHERE id = 'all'")
    fun getHistoryDoc(): Flow<ReviewHistoryEntity?>

    @Query("SELECT * FROM review_history WHERE id = 'all'")
    suspend fun getHistoryDocOnce(): ReviewHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistoryDoc(entity: ReviewHistoryEntity)

    @Query("SELECT * FROM review_state WHERE id = 'all'")
    fun getStateDoc(): Flow<ReviewStateEntity?>

    @Query("SELECT * FROM review_state WHERE id = 'all'")
    suspend fun getStateDocOnce(): ReviewStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStateDoc(entity: ReviewStateEntity)

    @Query("SELECT * FROM review_snapshot WHERE id = 'all'")
    fun getSnapshotDoc(): Flow<ReviewSnapshotEntity?>

    @Query("SELECT * FROM review_snapshot WHERE id = 'all'")
    suspend fun getSnapshotDocOnce(): ReviewSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshotDoc(entity: ReviewSnapshotEntity)

    @Query("SELECT * FROM review_stats WHERE id = 'all'")
    fun getStatsDoc(): Flow<ReviewStatsEntity?>

    @Query("SELECT * FROM review_stats WHERE id = 'all'")
    suspend fun getStatsDocOnce(): ReviewStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatsDoc(entity: ReviewStatsEntity)

    @Query("SELECT * FROM review_published WHERE id = 'all'")
    fun getPublishedDoc(): Flow<ReviewPublishedEntity?>

    @Query("SELECT * FROM review_published WHERE id = 'all'")
    suspend fun getPublishedDocOnce(): ReviewPublishedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPublishedDoc(entity: ReviewPublishedEntity)
}
