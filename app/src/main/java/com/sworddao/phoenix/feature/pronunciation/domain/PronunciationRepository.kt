package com.sworddao.phoenix.feature.pronunciation.domain

import com.sworddao.phoenix.feature.pronunciation.data.*
import kotlinx.coroutines.flow.Flow

interface PronunciationRepository {
    fun getAllExercises(): Flow<List<SpeakingExercise>>
    fun getExerciseById(exerciseId: String): Flow<SpeakingExercise?>
    fun getExercisesByType(type: SpeakingExerciseType): Flow<List<SpeakingExercise>>
    fun getExercisesByDifficulty(difficulty: SpeakingDifficulty): Flow<List<SpeakingExercise>>
    fun getExercisesByWord(wordId: String): Flow<List<SpeakingExercise>>
    fun getExercisesByPhrase(phraseId: String): Flow<List<SpeakingExercise>>
    fun getExercisesByNpc(npcId: String): Flow<List<SpeakingExercise>>
    fun getExercisesByQuest(questId: String): Flow<List<SpeakingExercise>>
    fun getUnlockedExercises(): Flow<List<SpeakingExercise>>
    fun getRecommendedExercises(limit: Int): Flow<List<SpeakingExercise>>

    fun getPronunciationProgress(wordId: String): Flow<PronunciationProgress?>
    fun getAllPronunciationProgress(): Flow<List<PronunciationProgress>>
    fun getSpeakingStatistics(): Flow<SpeakingStatistics>
    fun getPronunciationBadges(): Flow<List<PronunciationBadge>>

    suspend fun startSession(config: PronunciationSessionConfig): PronunciationSession
    suspend fun submitAttempt(attempt: PronunciationAttempt): PronunciationResultStatus
    suspend fun completeSession(session: PronunciationSession): PronunciationResultStatus
    suspend fun updateProgress(progress: PronunciationProgress): PronunciationResultStatus
    suspend fun unlockExercise(exerciseId: String): PronunciationResultStatus
    suspend fun recordStreak(streak: Int): PronunciationResultStatus
    suspend fun awardBadge(badgeId: String): PronunciationResultStatus
    suspend fun addExercises(exercises: List<SpeakingExercise>): PronunciationResultStatus

    suspend fun evaluatePronunciation(
        expectedText: String,
        expectedPinyin: String,
        spokenAudioPath: String
    ): PronunciationAttempt

    suspend fun evaluatePronunciationOffline(
        expectedText: String,
        expectedPinyin: String,
        spokenText: String
    ): PronunciationAttempt
}