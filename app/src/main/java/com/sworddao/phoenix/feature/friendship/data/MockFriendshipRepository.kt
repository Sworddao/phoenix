package com.sworddao.phoenix.feature.friendship.data

import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.npc.data.FriendshipLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockFriendshipRepository @Inject constructor() : FriendshipRepository {

    private val friendshipStates = MutableStateFlow<Map<String, FriendshipState>>(emptyMap())
    private val conversationMemories = MutableStateFlow<Map<String, MutableList<ConversationMemory>>>(emptyMap())
    private val friendshipEvents = MutableStateFlow<Map<String, MutableList<FriendshipEvent>>>(emptyMap())

    override fun getFriendshipState(npcId: String): Flow<FriendshipState?> {
        return friendshipStates.map { states -> states[npcId] }
    }

    override fun getAllFriendshipStates(): Flow<List<FriendshipState>> {
        return friendshipStates.map { states -> states.values.toList() }
    }

    override suspend fun addFriendshipXp(npcId: String, xpAmount: Int): FriendshipState? {
        var updatedState: FriendshipState? = null

        friendshipStates.update { currentStates ->
            val existing = currentStates[npcId] ?: FriendshipState(npcId = npcId)
            val newTotalXp = existing.friendshipXp + xpAmount
            val newLevel = FriendshipLevel.fromXp(newTotalXp)

            val hadLevelUp = newLevel != existing.friendshipLevel

            updatedState = existing.copy(
                friendshipXp = newTotalXp,
                friendshipLevel = newLevel,
                lastInteractionTimestamp = System.currentTimeMillis()
            )

            if (hadLevelUp) {
                val levelUpEvent = FriendshipEvent(
                    type = FriendshipEventType.LEVEL_UP,
                    npcId = npcId,
                    description = "Friendship level increased to ${newLevel.displayTitle}!",
                    xpChange = xpAmount,
                    metadata = mapOf("newLevel" to newLevel.name)
                )
                friendshipEvents.update { currentEvents ->
                    val events = currentEvents[npcId]?.toMutableList() ?: mutableListOf()
                    events.add(0, levelUpEvent)
                    currentEvents + (npcId to events)
                }
            }

            val xpEvent = FriendshipEvent(
                type = FriendshipEventType.CONVERSATION,
                npcId = npcId,
                description = "Gained $xpAmount friendship XP",
                xpChange = xpAmount
            )
            friendshipEvents.update { currentEvents ->
                val events = currentEvents[npcId]?.toMutableList() ?: mutableListOf()
                events.add(0, xpEvent)
                currentEvents + (npcId to events)
            }

            currentStates + (npcId to updatedState!!)
        }

        return updatedState
    }

    override suspend fun recordConversation(
        npcId: String,
        dialogueId: String,
        dialogueTitle: String,
        xpGained: Int,
        topicsDiscussed: List<String>,
        choicesSummary: List<String>
    ) {
        val memory = ConversationMemory(
            npcId = npcId,
            dialogueId = dialogueId,
            dialogueTitle = dialogueTitle,
            xpGained = xpGained,
            topicsDiscussed = topicsDiscussed,
            choicesSummary = choicesSummary
        )

        conversationMemories.update { currentMemories ->
            val memories = currentMemories[npcId]?.toMutableList() ?: mutableListOf()
            memories.add(0, memory)
            if (memories.size > 50) memories.removeAt(memories.lastIndex)
            currentMemories + (npcId to memories)
        }

        friendshipStates.update { currentStates ->
            val existing = currentStates[npcId] ?: FriendshipState(npcId = npcId)
            val updated = existing.copy(
                totalConversations = existing.totalConversations + 1,
                lastInteractionTimestamp = System.currentTimeMillis()
            )
            currentStates + (npcId to updated)
        }

        val event = FriendshipEvent(
            type = FriendshipEventType.CONVERSATION,
            npcId = npcId,
            description = "Had a conversation: $dialogueTitle",
            xpChange = xpGained
        )
        friendshipEvents.update { currentEvents ->
            val events = currentEvents[npcId]?.toMutableList() ?: mutableListOf()
            events.add(0, event)
            currentEvents + (npcId to events)
        }
    }

    override fun getConversationHistory(npcId: String): Flow<List<ConversationMemory>> {
        return conversationMemories.map { memories ->
            memories[npcId]?.sortedByDescending { it.timestamp } ?: emptyList()
        }
    }

    override fun getFriendshipEvents(npcId: String): Flow<List<FriendshipEvent>> {
        return friendshipEvents.map { events ->
            events[npcId]?.sortedByDescending { it.timestamp } ?: emptyList()
        }
    }

    override suspend fun unlockTopic(npcId: String, topic: String) {
        friendshipStates.update { currentStates ->
            val existing = currentStates[npcId] ?: FriendshipState(npcId = npcId)
            if (topic !in existing.unlockedTopics) {
                val updated = existing.copy(
                    unlockedTopics = existing.unlockedTopics + topic
                )
                val event = FriendshipEvent(
                    type = FriendshipEventType.TOPIC_UNLOCK,
                    npcId = npcId,
                    description = "Unlocked topic: $topic",
                    metadata = mapOf("topic" to topic)
                )
                friendshipEvents.update { currentEvents ->
                    val events = currentEvents[npcId]?.toMutableList() ?: mutableListOf()
                    events.add(0, event)
                    currentEvents + (npcId to events)
                }
                currentStates + (npcId to updated)
            } else {
                currentStates
            }
        }
    }

    override suspend fun initializeFriendship(npcId: String) {
        friendshipStates.update { currentStates ->
            if (npcId !in currentStates) {
                val initialState = FriendshipState(
                    npcId = npcId,
                    firstMeetingTimestamp = System.currentTimeMillis(),
                    lastInteractionTimestamp = System.currentTimeMillis()
                )
                val event = FriendshipEvent(
                    type = FriendshipEventType.FIRST_MEETING,
                    npcId = npcId,
                    description = "First meeting!"
                )
                friendshipEvents.update { currentEvents ->
                    val events = currentEvents[npcId]?.toMutableList() ?: mutableListOf()
                    events.add(0, event)
                    currentEvents + (npcId to events)
                }
                currentStates + (npcId to initialState)
            } else {
                currentStates
            }
        }
    }
}
