package com.sworddao.phoenix.feature.listening.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "listening_exercise")
data class ListeningExerciseEntity(
    @PrimaryKey val id: String,
    val type: String,
    val difficulty: String,
    val wordId: String? = null,
    val npcId: String? = null,
    val questId: String? = null,
    val isUnlocked: Boolean = true,
    val order: Int = 0,
    val xpReward: Int = 10,
    val exerciseJson: String,
)

@Entity(tableName = "listening_progress_doc")
data class ListeningProgressDocEntity(
    @PrimaryKey val id: String,
    val progressJson: String,
)

@Entity(tableName = "listening_statistics")
data class ListeningStatisticsEntity(
    @PrimaryKey val id: String,
    val statisticsJson: String,
)

@Entity(tableName = "listening_badges")
data class ListeningBadgesEntity(
    @PrimaryKey val id: String,
    val badgesJson: String,
)

@Entity(tableName = "listening_sessions")
data class ListeningSessionsEntity(
    @PrimaryKey val id: String,
    val activeSessionJson: String?,
    val completedSessionsJson: String,
)

@Entity(tableName = "listening_state")
data class ListeningStateEntity(
    @PrimaryKey val id: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastListeningDate: Long?,
    val correctCount: Int,
    val npcExerciseCount: Int,
    val practicedWordsJson: String,
    val recordedBadgeIdsJson: String,
    val replayCountsJson: String,
)

@Dao
interface ListeningDao {

    @Query("SELECT * FROM listening_exercise ORDER BY `order`")
    fun getAllExercises(): Flow<List<ListeningExerciseEntity>>

    @Query("SELECT * FROM listening_exercise WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: String): Flow<ListeningExerciseEntity?>

    @Query("SELECT * FROM listening_exercise WHERE type = :type ORDER BY `order`")
    fun getExercisesByType(type: String): Flow<List<ListeningExerciseEntity>>

    @Query("SELECT * FROM listening_exercise WHERE difficulty = :difficulty ORDER BY `order`")
    fun getExercisesByDifficulty(difficulty: String): Flow<List<ListeningExerciseEntity>>

    @Query("SELECT * FROM listening_exercise WHERE wordId = :wordId ORDER BY `order`")
    fun getExercisesByWord(wordId: String): Flow<List<ListeningExerciseEntity>>

    @Query("SELECT * FROM listening_exercise WHERE npcId = :npcId ORDER BY `order`")
    fun getExercisesByNpc(npcId: String): Flow<List<ListeningExerciseEntity>>

    @Query("SELECT * FROM listening_exercise WHERE questId = :questId ORDER BY `order`")
    fun getExercisesByQuest(questId: String): Flow<List<ListeningExerciseEntity>>

    @Query("SELECT * FROM listening_exercise WHERE isUnlocked = 1 ORDER BY `order`")
    fun getUnlockedExercises(): Flow<List<ListeningExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<ListeningExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ListeningExerciseEntity)

    @Query("SELECT COUNT(*) FROM listening_exercise")
    suspend fun countExercises(): Int

    @Query("UPDATE listening_exercise SET isUnlocked = 1 WHERE id = :exerciseId")
    suspend fun unlockExercise(exerciseId: String)

    @Query("SELECT * FROM listening_progress_doc WHERE id = 'all'")
    fun getProgressDoc(): Flow<ListeningProgressDocEntity?>

    @Query("SELECT * FROM listening_progress_doc WHERE id = 'all'")
    suspend fun getProgressDocOnce(): ListeningProgressDocEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgressDoc(entity: ListeningProgressDocEntity)

    @Query("SELECT * FROM listening_statistics WHERE id = 'all'")
    fun getStatisticsDoc(): Flow<ListeningStatisticsEntity?>

    @Query("SELECT * FROM listening_statistics WHERE id = 'all'")
    suspend fun getStatisticsDocOnce(): ListeningStatisticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatisticsDoc(entity: ListeningStatisticsEntity)

    @Query("SELECT * FROM listening_badges WHERE id = 'all'")
    fun getBadgesDoc(): Flow<ListeningBadgesEntity?>

    @Query("SELECT * FROM listening_badges WHERE id = 'all'")
    suspend fun getBadgesDocOnce(): ListeningBadgesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBadgesDoc(entity: ListeningBadgesEntity)

    @Query("SELECT * FROM listening_sessions WHERE id = 'all'")
    fun getSessionsDoc(): Flow<ListeningSessionsEntity?>

    @Query("SELECT * FROM listening_sessions WHERE id = 'all'")
    suspend fun getSessionsDocOnce(): ListeningSessionsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionsDoc(entity: ListeningSessionsEntity)

    @Query("SELECT * FROM listening_state WHERE id = 'all'")
    fun getStateDoc(): Flow<ListeningStateEntity?>

    @Query("SELECT * FROM listening_state WHERE id = 'all'")
    suspend fun getStateDocOnce(): ListeningStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStateDoc(entity: ListeningStateEntity)
}
