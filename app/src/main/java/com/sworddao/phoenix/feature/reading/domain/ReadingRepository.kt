package com.sworddao.phoenix.feature.reading.domain

import com.sworddao.phoenix.feature.reading.data.ReadingAttempt
import com.sworddao.phoenix.feature.reading.data.ReadingBadge
import com.sworddao.phoenix.feature.reading.data.ReadingDifficulty
import com.sworddao.phoenix.feature.reading.data.ReadingExercise
import com.sworddao.phoenix.feature.reading.data.ReadingExerciseType
import com.sworddao.phoenix.feature.reading.data.ReadingProgress
import com.sworddao.phoenix.feature.reading.data.ReadingResultStatus
import com.sworddao.phoenix.feature.reading.data.ReadingSession
import com.sworddao.phoenix.feature.reading.data.ReadingSessionConfig
import com.sworddao.phoenix.feature.reading.data.ReadingStatistics
import kotlinx.coroutines.flow.Flow

interface ReadingRepository {
    fun getAllExercises(): Flow<List<ReadingExercise>>
    fun getExerciseById(exerciseId: String): Flow<ReadingExercise?>
    fun getExercisesByType(type: ReadingExerciseType): Flow<List<ReadingExercise>>
    fun getExercisesByDifficulty(difficulty: ReadingDifficulty): Flow<List<ReadingExercise>>
    fun getExercisesByWord(wordId: String): Flow<List<ReadingExercise>>
    fun getExercisesByNpc(npcId: String): Flow<List<ReadingExercise>>
    fun getExercisesByQuest(questId: String): Flow<List<ReadingExercise>>
    fun getUnlockedExercises(): Flow<List<ReadingExercise>>
    fun getRecommendedExercises(limit: Int): Flow<List<ReadingExercise>>

    fun getReadingProgress(itemId: String): Flow<ReadingProgress?>
    fun getAllReadingProgress(): Flow<List<ReadingProgress>>
    fun getReadingStatistics(): Flow<ReadingStatistics>
    fun getReadingBadges(): Flow<List<ReadingBadge>>

    suspend fun startSession(config: ReadingSessionConfig): ReadingSession
    suspend fun submitAnswer(attempt: ReadingAttempt): ReadingResultStatus
    suspend fun completeSession(session: ReadingSession): ReadingResultStatus
    suspend fun updateProgress(progress: ReadingProgress): ReadingResultStatus
    suspend fun unlockExercise(exerciseId: String): ReadingResultStatus
    suspend fun recordStreak(streak: Int): ReadingResultStatus
    suspend fun awardBadge(badgeId: String): ReadingResultStatus
    suspend fun addExercises(exercises: List<ReadingExercise>): ReadingResultStatus
    suspend fun recordReveal(wordId: String): ReadingResultStatus
}
