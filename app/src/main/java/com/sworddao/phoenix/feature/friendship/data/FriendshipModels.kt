package com.sworddao.phoenix.feature.friendship.data

import com.sworddao.phoenix.feature.npc.data.FriendshipLevel

data class FriendshipState(
    val npcId: String,
    val friendshipXp: Int = 0,
    val friendshipLevel: FriendshipLevel = FriendshipLevel.STRANGER,
    val totalConversations: Int = 0,
    val firstMeetingTimestamp: Long = System.currentTimeMillis(),
    val lastInteractionTimestamp: Long = 0L,
    val unlockedTopics: List<String> = emptyList(),
    val recentGifts: List<GiftRecord> = emptyList(),
    val completedQuests: List<String> = emptyList()
)

data class GiftRecord(
    val giftId: String,
    val giftName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val friendshipXpBonus: Int = 0
)

data class RelationshipHistory(
    val npcId: String,
    val events: List<FriendshipEvent> = emptyList()
)

data class FriendshipEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: FriendshipEventType,
    val npcId: String,
    val description: String,
    val xpChange: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

enum class FriendshipEventType {
    CONVERSATION,
    GIFT,
    QUEST_COMPLETE,
    LEVEL_UP,
    TOPIC_UNLOCK,
    FIRST_MEETING,
    DAILY_VISIT
}

data class ConversationMemory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val npcId: String,
    val dialogueId: String,
    val dialogueTitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val topicsDiscussed: List<String> = emptyList(),
    val xpGained: Int = 0,
    val choicesSummary: List<String> = emptyList()
)

data class FriendshipAction(
    val type: FriendshipActionType,
    val targetNpcId: String,
    val value: String = ""
)

enum class FriendshipActionType {
    ADD_XP,
    UNLOCK_TOPIC,
    COMPLETE_QUEST,
    RECORD_GIFT,
    RECORD_CONVERSATION
}

data class NpcProfileState(
    val npcId: String = "",
    val displayName: String = "",
    val occupation: String = "",
    val personality: String = "",
    val avatarEmoji: String = "",
    val shortDescription: String = "",
    val friendshipState: FriendshipState = FriendshipState(""),
    val recentConversations: List<ConversationMemory> = emptyList(),
    val relationshipEvents: List<FriendshipEvent> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
