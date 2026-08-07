package com.sworddao.phoenix.feature.reading.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {

    @Query("SELECT * FROM reading_exercise ORDER BY `order`")
    fun getAllExercises(): Flow<List<ReadingExerciseEntity>>

    @Query("SELECT * FROM reading_exercise WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: String): Flow<ReadingExerciseEntity?>

    @Query("SELECT * FROM reading_exercise WHERE type = :type ORDER BY `order`")
    fun getExercisesByType(type: String): Flow<List<ReadingExerciseEntity>>

    @Query("SELECT * FROM reading_exercise WHERE difficulty = :difficulty ORDER BY `order`")
    fun getExercisesByDifficulty(difficulty: String): Flow<List<ReadingExerciseEntity>>

    @Query("SELECT * FROM reading_exercise WHERE wordId = :wordId ORDER BY `order`")
    fun getExercisesByWord(wordId: String): Flow<List<ReadingExerciseEntity>>

    @Query("SELECT * FROM reading_exercise WHERE npcId = :npcId ORDER BY `order`")
    fun getExercisesByNpc(npcId: String): Flow<List<ReadingExerciseEntity>>

    @Query("SELECT * FROM reading_exercise WHERE questId = :questId ORDER BY `order`")
    fun getExercisesByQuest(questId: String): Flow<List<ReadingExerciseEntity>>

    @Query("SELECT * FROM reading_exercise WHERE isUnlocked = 1 ORDER BY `order`")
    fun getUnlockedExercises(): Flow<List<ReadingExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<ReadingExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ReadingExerciseEntity)

    @Query("SELECT COUNT(*) FROM reading_exercise")
    suspend fun countExercises(): Int

    @Query("UPDATE reading_exercise SET isUnlocked = 1 WHERE id = :exerciseId")
    suspend fun unlockExercise(exerciseId: String)

    @Query("SELECT * FROM reading_progress_doc WHERE id = 'all'")
    fun getProgressDoc(): Flow<ReadingProgressDocEntity?>

    @Query("SELECT * FROM reading_progress_doc WHERE id = 'all'")
    suspend fun getProgressDocOnce(): ReadingProgressDocEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgressDoc(entity: ReadingProgressDocEntity)

    @Query("SELECT * FROM reading_statistics WHERE id = 'all'")
    fun getStatisticsDoc(): Flow<ReadingStatisticsEntity?>

    @Query("SELECT * FROM reading_statistics WHERE id = 'all'")
    suspend fun getStatisticsDocOnce(): ReadingStatisticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatisticsDoc(entity: ReadingStatisticsEntity)

    @Query("SELECT * FROM reading_badges WHERE id = 'all'")
    fun getBadgesDoc(): Flow<ReadingBadgesEntity?>

    @Query("SELECT * FROM reading_badges WHERE id = 'all'")
    suspend fun getBadgesDocOnce(): ReadingBadgesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBadgesDoc(entity: ReadingBadgesEntity)

    @Query("SELECT * FROM reading_sessions WHERE id = 'all'")
    fun getSessionsDoc(): Flow<ReadingSessionsEntity?>

    @Query("SELECT * FROM reading_sessions WHERE id = 'all'")
    suspend fun getSessionsDocOnce(): ReadingSessionsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionsDoc(entity: ReadingSessionsEntity)

    @Query("SELECT * FROM reading_state WHERE id = 'all'")
    fun getStateDoc(): Flow<ReadingStateEntity?>

    @Query("SELECT * FROM reading_state WHERE id = 'all'")
    suspend fun getStateDocOnce(): ReadingStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStateDoc(entity: ReadingStateEntity)
}
