package com.sworddao.phoenix.feature.writing.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WritingDao {

    @Query("SELECT * FROM writing_exercise ORDER BY `order`")
    fun getAllExercises(): Flow<List<WritingExerciseEntity>>

    @Query("SELECT * FROM writing_exercise WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: String): Flow<WritingExerciseEntity?>

    @Query("SELECT * FROM writing_exercise WHERE type = :type ORDER BY `order`")
    fun getExercisesByType(type: String): Flow<List<WritingExerciseEntity>>

    @Query("SELECT * FROM writing_exercise WHERE difficulty = :difficulty ORDER BY `order`")
    fun getExercisesByDifficulty(difficulty: String): Flow<List<WritingExerciseEntity>>

    @Query("SELECT * FROM writing_exercise WHERE wordId = :wordId ORDER BY `order`")
    fun getExercisesByWord(wordId: String): Flow<List<WritingExerciseEntity>>

    @Query("SELECT * FROM writing_exercise WHERE isUnlocked = 1 ORDER BY `order`")
    fun getUnlockedExercises(): Flow<List<WritingExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<WritingExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: WritingExerciseEntity)

    @Query("SELECT COUNT(*) FROM writing_exercise")
    suspend fun countExercises(): Int

    @Query("UPDATE writing_exercise SET isUnlocked = 1 WHERE id = :exerciseId")
    suspend fun unlockExercise(exerciseId: String)

    @Query("SELECT * FROM writing_progress_doc WHERE id = 'all'")
    fun getProgressDoc(): Flow<WritingProgressDocEntity?>

    @Query("SELECT * FROM writing_progress_doc WHERE id = 'all'")
    suspend fun getProgressDocOnce(): WritingProgressDocEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgressDoc(entity: WritingProgressDocEntity)

    @Query("SELECT * FROM writing_statistics WHERE id = 'all'")
    fun getStatisticsDoc(): Flow<WritingStatisticsEntity?>

    @Query("SELECT * FROM writing_statistics WHERE id = 'all'")
    suspend fun getStatisticsDocOnce(): WritingStatisticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatisticsDoc(entity: WritingStatisticsEntity)

    @Query("SELECT * FROM writing_badges WHERE id = 'all'")
    fun getBadgesDoc(): Flow<WritingBadgesEntity?>

    @Query("SELECT * FROM writing_badges WHERE id = 'all'")
    suspend fun getBadgesDocOnce(): WritingBadgesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBadgesDoc(entity: WritingBadgesEntity)

    @Query("SELECT * FROM writing_sessions WHERE id = 'all'")
    fun getSessionsDoc(): Flow<WritingSessionsEntity?>

    @Query("SELECT * FROM writing_sessions WHERE id = 'all'")
    suspend fun getSessionsDocOnce(): WritingSessionsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionsDoc(entity: WritingSessionsEntity)

    @Query("SELECT * FROM writing_state WHERE id = 'all'")
    fun getStateDoc(): Flow<WritingStateEntity?>

    @Query("SELECT * FROM writing_state WHERE id = 'all'")
    suspend fun getStateDocOnce(): WritingStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStateDoc(entity: WritingStateEntity)
}
