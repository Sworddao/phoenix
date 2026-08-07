package com.sworddao.phoenix.feature.reading.data

import com.sworddao.phoenix.data.local.RoomJson

fun ReadingExercise.toEntity(): ReadingExerciseEntity = ReadingExerciseEntity(
    id = id,
    type = type.name,
    difficulty = difficulty.name,
    wordId = relatedWordId,
    npcId = relatedNpcId,
    questId = relatedQuestId,
    isUnlocked = isUnlocked,
    order = order,
    xpReward = xpReward,
    exerciseJson = RoomJson.toJson(this),
)

fun ReadingExerciseEntity.toDomain(): ReadingExercise =
    RoomJson.fromJsonOrNull<ReadingExercise>(exerciseJson) ?: ReadingExercise(
        id = id,
        type = ReadingExerciseType.valueOf(type),
        difficulty = ReadingDifficulty.valueOf(difficulty),
        hanzi = "",
        pinyin = "",
        english = "",
        prompt = "",
        isUnlocked = isUnlocked,
        order = order,
    )

fun ReadingStateEntity.toDomain(
    readWords: Set<String> = emptySet(),
    recordedBadgeIds: Set<String> = emptySet(),
): ReadingState = ReadingState(
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastReadingDate = lastReadingDate,
    correctCount = correctCount,
    npcExerciseCount = npcExerciseCount,
    readWords = readWords,
    recordedBadgeIds = recordedBadgeIds,
    firstReadingRecorded = firstReadingRecorded,
)

fun ReadingState.toEntity(): ReadingStateEntity = ReadingStateEntity(
    id = "all",
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastReadingDate = lastReadingDate,
    correctCount = correctCount,
    npcExerciseCount = npcExerciseCount,
    readWordsJson = RoomJson.toJsonList(readWords.toList()),
    recordedBadgeIdsJson = RoomJson.toJsonList(recordedBadgeIds.toList()),
    firstReadingRecorded = firstReadingRecorded,
)

data class ReadingState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastReadingDate: Long? = null,
    val correctCount: Int = 0,
    val npcExerciseCount: Int = 0,
    val readWords: Set<String> = emptySet(),
    val recordedBadgeIds: Set<String> = emptySet(),
    val firstReadingRecorded: Boolean = false,
)
