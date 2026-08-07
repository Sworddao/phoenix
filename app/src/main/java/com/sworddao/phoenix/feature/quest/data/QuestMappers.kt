package com.sworddao.phoenix.feature.quest.data

import com.sworddao.phoenix.data.local.RoomJson

fun Quest.toEntity(): QuestEntity = QuestEntity(
    id = id,
    title = title,
    description = description,
    type = type.name,
    difficulty = difficulty.name,
    status = status.name,
    category = category.name,
    objectivesJson = RoomJson.toJsonList(objectives),
    rewardsJson = RoomJson.toJson(rewards),
    prerequisitesJson = RoomJson.toJson(prerequisites),
    npcId = npcId,
    locationId = locationId,
    dialogueId = dialogueId,
    repeatable = repeatable,
    daily = daily,
    timeLimitMinutes = timeLimitMinutes,
    completionDialogue = completionDialogue,
    failureDialogue = failureDialogue,
    order = order,
    chapter = chapter,
)

fun QuestEntity.toDomain(): Quest = Quest(
    id = id,
    title = title,
    description = description,
    type = QuestType.valueOf(type),
    difficulty = QuestDifficulty.valueOf(difficulty),
    status = QuestStatus.valueOf(status),
    category = QuestCategory.valueOf(category),
    objectives = RoomJson.fromJsonList(objectivesJson),
    rewards = RoomJson.fromJsonOrNull<QuestReward>(rewardsJson) ?: QuestReward(),
    prerequisites = RoomJson.fromJsonOrNull<QuestPrerequisite>(prerequisitesJson) ?: QuestPrerequisite(),
    npcId = npcId,
    locationId = locationId,
    dialogueId = dialogueId,
    repeatable = repeatable,
    daily = daily,
    timeLimitMinutes = timeLimitMinutes,
    completionDialogue = completionDialogue,
    failureDialogue = failureDialogue,
    order = order,
    chapter = chapter,
)

fun QuestProgress.toEntity(): QuestProgressEntity = QuestProgressEntity(
    questId = questId,
    status = status.name,
    objectivesJson = RoomJson.toJsonList(objectives),
    startedAt = startedAt,
    completedAt = completedAt,
    lastPlayedAt = lastPlayedAt,
    attempts = attempts,
)

fun QuestProgressEntity.toDomain(): QuestProgress = QuestProgress(
    questId = questId,
    status = QuestStatus.valueOf(status),
    objectives = RoomJson.fromJsonList(objectivesJson),
    startedAt = startedAt,
    completedAt = completedAt,
    lastPlayedAt = lastPlayedAt,
    attempts = attempts,
)
