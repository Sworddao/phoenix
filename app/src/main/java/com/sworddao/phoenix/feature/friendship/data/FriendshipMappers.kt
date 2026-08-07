package com.sworddao.phoenix.feature.friendship.data

import com.sworddao.phoenix.data.local.RoomJson
import com.sworddao.phoenix.feature.npc.data.FriendshipLevel

fun FriendshipEntity.toDomainModel(): FriendshipState {
    return FriendshipState(
        npcId = npcId,
        friendshipXp = friendshipXp,
        friendshipLevel = FriendshipLevel.valueOf(friendshipLevel),
        totalConversations = totalConversations,
        firstMeetingTimestamp = firstMeetingTimestamp,
        lastInteractionTimestamp = lastInteractionTimestamp,
        unlockedTopics = if (unlockedTopics.isBlank()) emptyList() else unlockedTopics.split(","),
        recentGifts = RoomJson.fromJsonList(recentGifts),
        completedQuests = if (completedQuests.isBlank()) emptyList() else completedQuests.split(",")
    )
}

fun FriendshipState.toEntity(): FriendshipEntity {
    return FriendshipEntity(
        npcId = npcId,
        friendshipXp = friendshipXp,
        friendshipLevel = friendshipLevel.name,
        totalConversations = totalConversations,
        firstMeetingTimestamp = firstMeetingTimestamp,
        lastInteractionTimestamp = lastInteractionTimestamp,
        unlockedTopics = unlockedTopics.joinToString(","),
        recentGifts = RoomJson.toJsonList(recentGifts),
        completedQuests = completedQuests.joinToString(",")
    )
}

fun ConversationMemoryEntity.toDomainModel(): ConversationMemory {
    return ConversationMemory(
        id = id,
        npcId = npcId,
        dialogueId = dialogueId,
        dialogueTitle = dialogueTitle,
        timestamp = timestamp,
        durationSeconds = durationSeconds,
        topicsDiscussed = if (topicsDiscussed.isBlank()) emptyList() else topicsDiscussed.split(","),
        xpGained = xpGained,
        choicesSummary = if (choicesSummary.isBlank()) emptyList() else choicesSummary.split(",")
    )
}

fun ConversationMemory.toEntity(): ConversationMemoryEntity {
    return ConversationMemoryEntity(
        id = id,
        npcId = npcId,
        dialogueId = dialogueId,
        dialogueTitle = dialogueTitle,
        timestamp = timestamp,
        durationSeconds = durationSeconds,
        topicsDiscussed = topicsDiscussed.joinToString(","),
        xpGained = xpGained,
        choicesSummary = choicesSummary.joinToString(",")
    )
}

fun FriendshipEventEntity.toDomainModel(): FriendshipEvent {
    return FriendshipEvent(
        id = id,
        type = FriendshipEventType.valueOf(type),
        npcId = npcId,
        description = description,
        xpChange = xpChange,
        timestamp = timestamp,
        metadata = RoomJson.fromJsonMap(metadata)
    )
}

fun FriendshipEvent.toEntity(): FriendshipEventEntity {
    return FriendshipEventEntity(
        id = id,
        type = type.name,
        npcId = npcId,
        description = description,
        xpChange = xpChange,
        timestamp = timestamp,
        metadata = RoomJson.toJson(metadata)
    )
}
