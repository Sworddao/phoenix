package com.sworddao.phoenix.feature.friendship.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friendship_state")
data class FriendshipEntity(
    @PrimaryKey
    val npcId: String,
    val friendshipXp: Int = 0,
    val friendshipLevel: String = "STRANGER",
    val totalConversations: Int = 0,
    val firstMeetingTimestamp: Long = System.currentTimeMillis(),
    val lastInteractionTimestamp: Long = 0L,
    val unlockedTopics: String = "",
    val recentGifts: String = "",
    val completedQuests: String = ""
)

@Entity(tableName = "conversation_memory")
data class ConversationMemoryEntity(
    @PrimaryKey
    val id: String,
    val npcId: String,
    val dialogueId: String,
    val dialogueTitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val topicsDiscussed: String = "",
    val xpGained: Int = 0,
    val choicesSummary: String = ""
)

@Entity(tableName = "friendship_event")
data class FriendshipEventEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val npcId: String,
    val description: String,
    val xpChange: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String = ""
)
