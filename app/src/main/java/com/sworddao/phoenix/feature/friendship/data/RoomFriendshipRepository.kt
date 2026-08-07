package com.sworddao.phoenix.feature.friendship.data

import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.npc.data.FriendshipLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomFriendshipRepository @Inject constructor(
    private val dao: FriendshipDao,
) : FriendshipRepository {

    override fun getFriendshipState(npcId: String): Flow<FriendshipState?> =
        dao.getFriendshipState(npcId).map { it?.toDomainModel() }

    override fun getAllFriendshipStates(): Flow<List<FriendshipState>> =
        dao.getAllFriendshipStates().map { list -> list.map { it.toDomainModel() } }

    override suspend fun addFriendshipXp(npcId: String, xpAmount: Int): FriendshipState? {
        val existing = dao.getFriendshipState(npcId).first()?.toDomainModel()
            ?: FriendshipState(npcId = npcId)
        val newTotalXp = existing.friendshipXp + xpAmount
        val newLevel = FriendshipLevel.fromXp(newTotalXp)
        val hadLevelUp = newLevel != existing.friendshipLevel

        val updated = existing.copy(
            friendshipXp = newTotalXp,
            friendshipLevel = newLevel,
            lastInteractionTimestamp = System.currentTimeMillis()
        )
        dao.upsertFriendshipState(updated.toEntity())

        if (hadLevelUp) {
            dao.insertFriendshipEvent(
                FriendshipEvent(
                    type = FriendshipEventType.LEVEL_UP,
                    npcId = npcId,
                    description = "Friendship level increased to ${newLevel.displayTitle}!",
                    xpChange = xpAmount,
                    metadata = mapOf("newLevel" to newLevel.name)
                ).toEntity()
            )
        }
        dao.insertFriendshipEvent(
            FriendshipEvent(
                type = FriendshipEventType.CONVERSATION,
                npcId = npcId,
                description = "Gained $xpAmount friendship XP",
                xpChange = xpAmount
            ).toEntity()
        )

        return updated
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
        dao.insertConversationMemory(memory.toEntity())

        val existing = dao.getFriendshipState(npcId).first()?.toDomainModel()
            ?: FriendshipState(npcId = npcId)
        dao.upsertFriendshipState(
            existing.copy(
                totalConversations = existing.totalConversations + 1,
                lastInteractionTimestamp = System.currentTimeMillis()
            ).toEntity()
        )

        dao.insertFriendshipEvent(
            FriendshipEvent(
                type = FriendshipEventType.CONVERSATION,
                npcId = npcId,
                description = "Had a conversation: $dialogueTitle",
                xpChange = xpGained
            ).toEntity()
        )
    }

    override fun getConversationHistory(npcId: String): Flow<List<ConversationMemory>> =
        dao.getConversationHistory(npcId).map { list -> list.map { it.toDomainModel() } }

    override fun getFriendshipEvents(npcId: String): Flow<List<FriendshipEvent>> =
        dao.getFriendshipEvents(npcId).map { list -> list.map { it.toDomainModel() } }

    override suspend fun unlockTopic(npcId: String, topic: String) {
        val existing = dao.getFriendshipState(npcId).first()?.toDomainModel()
            ?: FriendshipState(npcId = npcId)
        if (topic !in existing.unlockedTopics) {
            dao.upsertFriendshipState(
                existing.copy(unlockedTopics = existing.unlockedTopics + topic).toEntity()
            )
            dao.insertFriendshipEvent(
                FriendshipEvent(
                    type = FriendshipEventType.TOPIC_UNLOCK,
                    npcId = npcId,
                    description = "Unlocked topic: $topic",
                    metadata = mapOf("topic" to topic)
                ).toEntity()
            )
        }
    }

    override suspend fun initializeFriendship(npcId: String) {
        val existing = dao.getFriendshipState(npcId).first()
        if (existing == null) {
            val initialState = FriendshipState(
                npcId = npcId,
                firstMeetingTimestamp = System.currentTimeMillis(),
                lastInteractionTimestamp = System.currentTimeMillis()
            )
            dao.upsertFriendshipState(initialState.toEntity())
            dao.insertFriendshipEvent(
                FriendshipEvent(
                    type = FriendshipEventType.FIRST_MEETING,
                    npcId = npcId,
                    description = "First meeting!"
                ).toEntity()
            )
        }
    }
}
