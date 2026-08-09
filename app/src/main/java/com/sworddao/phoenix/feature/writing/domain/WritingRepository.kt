package com.sworddao.phoenix.feature.writing.domain

import com.sworddao.phoenix.feature.writing.data.WritingAttempt
import com.sworddao.phoenix.feature.writing.data.WritingBadge
import com.sworddao.phoenix.feature.writing.data.WritingDifficulty
import com.sworddao.phoenix.feature.writing.data.WritingExercise
import com.sworddao.phoenix.feature.writing.data.WritingExerciseType
import com.sworddao.phoenix.feature.writing.data.WritingProgress
import com.sworddao.phoenix.feature.writing.data.WritingResultStatus
import com.sworddao.phoenix.feature.writing.data.WritingSession
import com.sworddao.phoenix.feature.writing.data.WritingSessionConfig
import com.sworddao.phoenix.feature.writing.data.WritingStatistics
import kotlinx.coroutines.flow.Flow

interface WritingRepository {
    fun getAllExercises(): Flow<List<WritingExercise>>
    fun getExerciseById(exerciseId: String): Flow<WritingExercise?>
    fun getExercisesByType(type: WritingExerciseType): Flow<List<WritingExercise>>
    fun getExercisesByDifficulty(difficulty: WritingDifficulty): Flow<List<WritingExercise>>
    fun getExercisesByWord(wordId: String): Flow<List<WritingExercise>>
    fun getUnlockedExercises(): Flow<List<WritingExercise>>
    fun getRecommendedExercises(limit: Int): Flow<List<WritingExercise>>

    fun getWritingProgress(itemId: String): Flow<WritingProgress?>
    fun getAllWritingProgress(): Flow<List<WritingProgress>>
    fun getWritingStatistics(): Flow<WritingStatistics>
    fun getWritingBadges(): Flow<List<WritingBadge>>

    suspend fun startSession(config: WritingSessionConfig): WritingSession
    suspend fun submitAnswer(attempt: WritingAttempt): WritingResultStatus
    suspend fun completeSession(session: WritingSession): WritingResultStatus
    suspend fun updateProgress(progress: WritingProgress): WritingResultStatus
    suspend fun unlockExercise(exerciseId: String): WritingResultStatus
    suspend fun recordStreak(streak: Int): WritingResultStatus
    suspend fun awardBadge(badgeId: String): WritingResultStatus
    suspend fun addExercises(exercises: List<WritingExercise>): WritingResultStatus
}
