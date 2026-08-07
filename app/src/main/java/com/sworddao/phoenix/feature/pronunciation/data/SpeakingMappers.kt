package com.sworddao.phoenix.feature.pronunciation.data

import com.sworddao.phoenix.data.local.RoomJson

fun SpeakingExercise.toEntity(): SpeakingExerciseEntity = SpeakingExerciseEntity(
    id = id,
    type = type.name,
    difficulty = difficulty.name,
    wordId = wordId,
    phraseId = phraseId,
    npcId = relatedNpcId,
    questId = relatedQuestId,
    isUnlocked = isUnlocked,
    order = order,
    xpReward = xpReward,
    exerciseJson = RoomJson.toJson(this),
)

fun SpeakingExerciseEntity.toDomain(): SpeakingExercise =
    RoomJson.fromJsonOrNull<SpeakingExercise>(exerciseJson) ?: SpeakingExercise(
        id = id,
        type = SpeakingExerciseType.valueOf(type),
        difficulty = SpeakingDifficulty.valueOf(difficulty),
        expectedText = "",
        expectedPinyin = "",
        isUnlocked = isUnlocked,
        order = order,
    )

data class SpeakingState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastPracticeDate: Long? = null,
    val practicedWords: Set<String> = emptySet(),
    val highConfidenceWords: Set<String> = emptySet(),
    val perfectToneExercises: Set<String> = emptySet(),
    val dialoguePhraseExercises: Set<String> = emptySet(),
    val attemptedExerciseIds: Set<String> = emptySet(),
    val practiceCountByType: Map<SpeakingExerciseType, Int> = emptyMap(),
    val practiceCountByDifficulty: Map<SpeakingDifficulty, Int> = emptyMap(),
    val recordedBadgeIds: Set<String> = emptySet(),
    val lastConfidenceByKey: Map<String, Float> = emptyMap(),
    val confidenceSum: Float = 0f,
    val toneSum: Float = 0f,
    val fluencySum: Float = 0f,
)

fun SpeakingState.toEntity(): SpeakingStateEntity = SpeakingStateEntity(
    id = "all",
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastPracticeDate = lastPracticeDate,
    practicedWordsJson = RoomJson.toJsonList(practicedWords.toList()),
    highConfidenceWordsJson = RoomJson.toJsonList(highConfidenceWords.toList()),
    perfectToneExercisesJson = RoomJson.toJsonList(perfectToneExercises.toList()),
    dialoguePhraseExercisesJson = RoomJson.toJsonList(dialoguePhraseExercises.toList()),
    attemptedExerciseIdsJson = RoomJson.toJsonList(attemptedExerciseIds.toList()),
    practiceCountByTypeJson = RoomJson.toJson(practiceCountByType),
    practiceCountByDifficultyJson = RoomJson.toJson(practiceCountByDifficulty),
    recordedBadgeIdsJson = RoomJson.toJsonList(recordedBadgeIds.toList()),
    lastConfidenceByKeyJson = RoomJson.toJson(lastConfidenceByKey),
    confidenceSum = confidenceSum,
    toneSum = toneSum,
    fluencySum = fluencySum,
)

fun SpeakingStateEntity.toDomain(): SpeakingState = SpeakingState(
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastPracticeDate = lastPracticeDate,
    practicedWords = RoomJson.fromJsonList<String>(practicedWordsJson).toSet(),
    highConfidenceWords = RoomJson.fromJsonList<String>(highConfidenceWordsJson).toSet(),
    perfectToneExercises = RoomJson.fromJsonList<String>(perfectToneExercisesJson).toSet(),
    dialoguePhraseExercises = RoomJson.fromJsonList<String>(dialoguePhraseExercisesJson).toSet(),
    attemptedExerciseIds = RoomJson.fromJsonList<String>(attemptedExerciseIdsJson).toSet(),
    practiceCountByType = RoomJson.fromJsonOrNull<Map<SpeakingExerciseType, Int>>(practiceCountByTypeJson) ?: emptyMap(),
    practiceCountByDifficulty = RoomJson.fromJsonOrNull<Map<SpeakingDifficulty, Int>>(practiceCountByDifficultyJson) ?: emptyMap(),
    recordedBadgeIds = RoomJson.fromJsonList<String>(recordedBadgeIdsJson).toSet(),
    lastConfidenceByKey = RoomJson.fromJsonMap(lastConfidenceByKeyJson),
    confidenceSum = confidenceSum,
    toneSum = toneSum,
    fluencySum = fluencySum,
)
