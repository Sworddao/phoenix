package com.sworddao.phoenix.feature.listening.data

import com.sworddao.phoenix.data.local.RoomJson

fun ListeningExercise.toEntity(): ListeningExerciseEntity = ListeningExerciseEntity(
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

fun ListeningExerciseEntity.toDomain(): ListeningExercise =
    RoomJson.fromJsonOrNull<ListeningExercise>(exerciseJson) ?: ListeningExercise(
        id = id,
        type = ListeningExerciseType.valueOf(type),
        difficulty = ListeningDifficulty.valueOf(difficulty),
        clip = AudioClip(id = id, text = "", english = ""),
        prompt = "",
        choices = emptyList(),
        isUnlocked = isUnlocked,
        order = order,
    )

data class ListeningState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastListeningDate: Long? = null,
    val correctCount: Int = 0,
    val npcExerciseCount: Int = 0,
    val practicedWords: Set<String> = emptySet(),
    val recordedBadgeIds: Set<String> = emptySet(),
    val replayCounts: Map<String, Int> = emptyMap(),
)

fun ListeningState.toEntity(): ListeningStateEntity = ListeningStateEntity(
    id = "all",
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastListeningDate = lastListeningDate,
    correctCount = correctCount,
    npcExerciseCount = npcExerciseCount,
    practicedWordsJson = RoomJson.toJsonList(practicedWords.toList()),
    recordedBadgeIdsJson = RoomJson.toJsonList(recordedBadgeIds.toList()),
    replayCountsJson = RoomJson.toJson(replayCounts),
)

fun ListeningStateEntity.toDomain(): ListeningState = ListeningState(
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastListeningDate = lastListeningDate,
    correctCount = correctCount,
    npcExerciseCount = npcExerciseCount,
    practicedWords = RoomJson.fromJsonList<String>(practicedWordsJson).toSet(),
    recordedBadgeIds = RoomJson.fromJsonList<String>(recordedBadgeIdsJson).toSet(),
    replayCounts = RoomJson.fromJsonMap(replayCountsJson),
)
