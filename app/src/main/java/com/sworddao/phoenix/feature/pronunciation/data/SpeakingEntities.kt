package com.sworddao.phoenix.feature.pronunciation.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "speaking_exercise")
data class SpeakingExerciseEntity(
    @PrimaryKey val id: String,
    val type: String,
    val difficulty: String,
    val wordId: String? = null,
    val phraseId: String? = null,
    val npcId: String? = null,
    val questId: String? = null,
    val isUnlocked: Boolean = true,
    val order: Int = 0,
    val xpReward: Int = 10,
    val exerciseJson: String,
)

@Entity(tableName = "speaking_progress_doc")
data class SpeakingProgressDocEntity(
    @PrimaryKey val id: String,
    val progressJson: String,
)

@Entity(tableName = "speaking_statistics")
data class SpeakingStatisticsEntity(
    @PrimaryKey val id: String,
    val statisticsJson: String,
)

@Entity(tableName = "speaking_badges")
data class SpeakingBadgesEntity(
    @PrimaryKey val id: String,
    val badgesJson: String,
)

@Entity(tableName = "speaking_sessions")
data class SpeakingSessionsEntity(
    @PrimaryKey val id: String,
    val activeSessionJson: String?,
    val completedSessionsJson: String,
)

@Entity(tableName = "speaking_state")
data class SpeakingStateEntity(
    @PrimaryKey val id: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastPracticeDate: Long?,
    val practicedWordsJson: String,
    val highConfidenceWordsJson: String,
    val perfectToneExercisesJson: String,
    val dialoguePhraseExercisesJson: String,
    val attemptedExerciseIdsJson: String,
    val practiceCountByTypeJson: String,
    val practiceCountByDifficultyJson: String,
    val recordedBadgeIdsJson: String,
    val lastConfidenceByKeyJson: String,
    val confidenceSum: Float,
    val toneSum: Float,
    val fluencySum: Float,
)

@Dao
interface SpeakingDao {

    @Query("SELECT * FROM speaking_exercise ORDER BY `order`")
    fun getAllExercises(): Flow<List<SpeakingExerciseEntity>>

    @Query("SELECT * FROM speaking_exercise WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: String): Flow<SpeakingExerciseEntity?>

    @Query("SELECT * FROM speaking_exercise WHERE id = :exerciseId")
    suspend fun getExerciseByIdOnce(exerciseId: String): SpeakingExerciseEntity?

    @Query("SELECT * FROM speaking_exercise WHERE type = :type ORDER BY `order`")
    fun getExercisesByType(type: String): Flow<List<SpeakingExerciseEntity>>

    @Query("SELECT * FROM speaking_exercise WHERE difficulty = :difficulty ORDER BY `order`")
    fun getExercisesByDifficulty(difficulty: String): Flow<List<SpeakingExerciseEntity>>

    @Query("SELECT * FROM speaking_exercise WHERE wordId = :wordId ORDER BY `order`")
    fun getExercisesByWord(wordId: String): Flow<List<SpeakingExerciseEntity>>

    @Query("SELECT * FROM speaking_exercise WHERE phraseId = :phraseId ORDER BY `order`")
    fun getExercisesByPhrase(phraseId: String): Flow<List<SpeakingExerciseEntity>>

    @Query("SELECT * FROM speaking_exercise WHERE npcId = :npcId ORDER BY `order`")
    fun getExercisesByNpc(npcId: String): Flow<List<SpeakingExerciseEntity>>

    @Query("SELECT * FROM speaking_exercise WHERE questId = :questId ORDER BY `order`")
    fun getExercisesByQuest(questId: String): Flow<List<SpeakingExerciseEntity>>

    @Query("SELECT * FROM speaking_exercise WHERE isUnlocked = 1 ORDER BY `order`")
    fun getUnlockedExercises(): Flow<List<SpeakingExerciseEntity>>

    @Query("SELECT COUNT(*) FROM speaking_exercise")
    suspend fun countExercises(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<SpeakingExerciseEntity>)

    @Query("UPDATE speaking_exercise SET isUnlocked = 1 WHERE id = :exerciseId")
    suspend fun unlockExercise(exerciseId: String)

    @Query("SELECT * FROM speaking_progress_doc WHERE id = 'all'")
    fun getProgressDoc(): Flow<SpeakingProgressDocEntity?>

    @Query("SELECT * FROM speaking_progress_doc WHERE id = 'all'")
    suspend fun getProgressDocOnce(): SpeakingProgressDocEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgressDoc(entity: SpeakingProgressDocEntity)

    @Query("SELECT * FROM speaking_statistics WHERE id = 'all'")
    fun getStatisticsDoc(): Flow<SpeakingStatisticsEntity?>

    @Query("SELECT * FROM speaking_statistics WHERE id = 'all'")
    suspend fun getStatisticsDocOnce(): SpeakingStatisticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatisticsDoc(entity: SpeakingStatisticsEntity)

    @Query("SELECT * FROM speaking_badges WHERE id = 'all'")
    fun getBadgesDoc(): Flow<SpeakingBadgesEntity?>

    @Query("SELECT * FROM speaking_badges WHERE id = 'all'")
    suspend fun getBadgesDocOnce(): SpeakingBadgesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBadgesDoc(entity: SpeakingBadgesEntity)

    @Query("SELECT * FROM speaking_sessions WHERE id = 'all'")
    fun getSessionsDoc(): Flow<SpeakingSessionsEntity?>

    @Query("SELECT * FROM speaking_sessions WHERE id = 'all'")
    suspend fun getSessionsDocOnce(): SpeakingSessionsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionsDoc(entity: SpeakingSessionsEntity)

    @Query("SELECT * FROM speaking_state WHERE id = 'all'")
    fun getStateDoc(): Flow<SpeakingStateEntity?>

    @Query("SELECT * FROM speaking_state WHERE id = 'all'")
    suspend fun getStateDocOnce(): SpeakingStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStateDoc(entity: SpeakingStateEntity)
}
