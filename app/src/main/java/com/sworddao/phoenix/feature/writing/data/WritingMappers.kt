package com.sworddao.phoenix.feature.writing.data

import com.sworddao.phoenix.data.local.RoomJson

fun WritingExercise.toEntity(): WritingExerciseEntity = WritingExerciseEntity(
    id = id,
    type = type.name,
    difficulty = difficulty.name,
    wordId = character.wordId,
    isUnlocked = isUnlocked,
    order = order,
    xpReward = xpReward,
    characterJson = RoomJson.toJson(character),
    exerciseJson = RoomJson.toJson(this),
)

fun WritingExerciseEntity.toDomain(): WritingExercise =
    (RoomJson.fromJsonOrNull<WritingExercise>(exerciseJson) ?: WritingExercise(
        id = id,
        type = WritingExerciseType.valueOf(type),
        difficulty = WritingDifficulty.valueOf(difficulty),
        character = RoomJson.fromJsonOrNull<HanziCharacter>(characterJson)
            ?: HanziCharacter(id = id, hanzi = "", pinyin = "", english = "", wordId = wordId),
        prompt = "",
        isUnlocked = isUnlocked,
        order = order,
    )).copy(isUnlocked = isUnlocked)

fun WritingStateEntity.toDomain(
    writtenCharacters: Set<String> = emptySet(),
    recordedBadgeIds: Set<String> = emptySet(),
): WritingState = WritingState(
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastWritingDate = lastWritingDate,
    correctCount = correctCount,
    writtenCharacters = writtenCharacters,
    recordedBadgeIds = recordedBadgeIds,
    firstWritingRecorded = firstWritingRecorded,
)

fun WritingState.toEntity(): WritingStateEntity = WritingStateEntity(
    id = "all",
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastWritingDate = lastWritingDate,
    correctCount = correctCount,
    writtenCharactersJson = RoomJson.toJsonList(writtenCharacters.toList()),
    recordedBadgeIdsJson = RoomJson.toJsonList(recordedBadgeIds.toList()),
    firstWritingRecorded = firstWritingRecorded,
)

data class WritingState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastWritingDate: Long? = null,
    val correctCount: Int = 0,
    val writtenCharacters: Set<String> = emptySet(),
    val recordedBadgeIds: Set<String> = emptySet(),
    val firstWritingRecorded: Boolean = false,
)
