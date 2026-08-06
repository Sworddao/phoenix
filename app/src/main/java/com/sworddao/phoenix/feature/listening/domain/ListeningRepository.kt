package com.sworddao.phoenix.feature.listening.domain

import com.sworddao.phoenix.feature.listening.data.ListeningAttempt
import com.sworddao.phoenix.feature.listening.data.ListeningBadge
import com.sworddao.phoenix.feature.listening.data.ListeningDifficulty
import com.sworddao.phoenix.feature.listening.data.ListeningExercise
import com.sworddao.phoenix.feature.listening.data.ListeningExerciseType
import com.sworddao.phoenix.feature.listening.data.ListeningProgress
import com.sworddao.phoenix.feature.listening.data.ListeningResultStatus
import com.sworddao.phoenix.feature.listening.data.ListeningSession
import com.sworddao.phoenix.feature.listening.data.ListeningSessionConfig
import com.sworddao.phoenix.feature.listening.data.ListeningStatistics
import kotlinx.coroutines.flow.Flow

interface ListeningRepository {
    fun getAllExercises(): Flow<List<ListeningExercise>>
    fun getExerciseById(exerciseId: String): Flow<ListeningExercise?>
    fun getExercisesByType(type: ListeningExerciseType): Flow<List<ListeningExercise>>
    fun getExercisesByDifficulty(difficulty: ListeningDifficulty): Flow<List<ListeningExercise>>
    fun getExercisesByWord(wordId: String): Flow<List<ListeningExercise>>
    fun getExercisesByNpc(npcId: String): Flow<List<ListeningExercise>>
    fun getExercisesByQuest(questId: String): Flow<List<ListeningExercise>>
    fun getUnlockedExercises(): Flow<List<ListeningExercise>>
    fun getRecommendedExercises(limit: Int): Flow<List<ListeningExercise>>

    fun getListeningProgress(itemId: String): Flow<ListeningProgress?>
    fun getAllListeningProgress(): Flow<List<ListeningProgress>>
    fun getListeningStatistics(): Flow<ListeningStatistics>
    fun getListeningBadges(): Flow<List<ListeningBadge>>

    suspend fun startSession(config: ListeningSessionConfig): ListeningSession
    suspend fun submitAnswer(attempt: ListeningAttempt): ListeningResultStatus
    suspend fun completeSession(session: ListeningSession): ListeningResultStatus
    suspend fun updateProgress(progress: ListeningProgress): ListeningResultStatus
    suspend fun unlockExercise(exerciseId: String): ListeningResultStatus
    suspend fun recordStreak(streak: Int): ListeningResultStatus
    suspend fun awardBadge(badgeId: String): ListeningResultStatus
    suspend fun addExercises(exercises: List<ListeningExercise>): ListeningResultStatus
    suspend fun recordReplay(exerciseId: String): ListeningResultStatus
}
