package com.sworddao.phoenix.feature.dialogue.data

import com.sworddao.phoenix.data.local.RoomJson

fun DialogueEntity.toDomain(): Dialogue = Dialogue(
    id = id,
    npcId = npcId,
    title = title,
    description = description,
    startNodeId = startNodeId,
    nodes = RoomJson.fromJsonList(nodesJson),
    requiredFriendshipLevel = requiredFriendshipLevel
)

fun Dialogue.toEntity(): DialogueEntity = DialogueEntity(
    id = id,
    npcId = npcId,
    title = title,
    description = description,
    startNodeId = startNodeId,
    requiredFriendshipLevel = requiredFriendshipLevel,
    nodesJson = RoomJson.toJsonList(nodes)
)
