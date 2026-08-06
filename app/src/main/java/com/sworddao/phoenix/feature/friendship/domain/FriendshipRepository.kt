package com.sworddao.phoenix.feature.friendship.domain

import com.sworddao.phoenix.feature.friendship.data.ConversationMemory
import com.sworddao.phoenix.feature.friendship.data.FriendshipEvent
import com.sworddao.phoenix.feature.friendship.data.FriendshipState
import kotlinx.coroutines.flow.Flow

interface FriendshipRepository {

    fun getFriendshipState(npcId: String): Flow<FriendshipState?>

    fun getAllFriendshipStates(): Flow<List<FriendshipState>>

    suspend fun addFriendshipXp(npcId: String, xpAmount: Int): FriendshipState?

    suspend fun recordConversation(
        npcId: String,
        dialogueId: String,
        dialogueTitle: String,
        xpGained: Int,
        topicsDiscussed: List<String> = emptyList(),
        choicesSummary: List<String> = emptyList()
    )

    fun getConversationHistory(npcId: String): Flow<List<ConversationMemory>>

    fun getFriendshipEvents(npcId: String): Flow<List<FriendshipEvent>>

    suspend fun unlockTopic(npcId: String, topic: String)

    suspend fun initializeFriendship(npcId: String)
}
