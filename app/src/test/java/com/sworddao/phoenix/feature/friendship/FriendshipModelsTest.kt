package com.sworddao.phoenix.feature.friendship

import com.sworddao.phoenix.feature.friendship.data.ConversationMemory
import com.sworddao.phoenix.feature.friendship.data.FriendshipEvent
import com.sworddao.phoenix.feature.friendship.data.FriendshipEventType
import com.sworddao.phoenix.feature.friendship.data.FriendshipState
import com.sworddao.phoenix.feature.friendship.data.GiftRecord
import com.sworddao.phoenix.feature.npc.data.FriendshipLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendshipModelsTest {

    @Test
    fun `FriendshipState has correct default values`() {
        val state = FriendshipState(npcId = "npc_1")

        assertEquals("npc_1", state.npcId)
        assertEquals(0, state.friendshipXp)
        assertEquals(FriendshipLevel.STRANGER, state.friendshipLevel)
        assertEquals(0, state.totalConversations)
        assertTrue(state.unlockedTopics.isEmpty())
        assertTrue(state.recentGifts.isEmpty())
        assertTrue(state.completedQuests.isEmpty())
    }

    @Test
    fun `FriendshipState has correct custom values`() {
        val state = FriendshipState(
            npcId = "npc_1",
            friendshipXp = 500,
            friendshipLevel = FriendshipLevel.FRIEND,
            totalConversations = 10,
            firstMeetingTimestamp = 1000L,
            lastInteractionTimestamp = 2000L,
            unlockedTopics = listOf("food", "greetings"),
            completedQuests = listOf("quest_1")
        )

        assertEquals("npc_1", state.npcId)
        assertEquals(500, state.friendshipXp)
        assertEquals(FriendshipLevel.FRIEND, state.friendshipLevel)
        assertEquals(10, state.totalConversations)
        assertEquals(1000L, state.firstMeetingTimestamp)
        assertEquals(2000L, state.lastInteractionTimestamp)
        assertEquals(listOf("food", "greetings"), state.unlockedTopics)
        assertEquals(listOf("quest_1"), state.completedQuests)
    }

    @Test
    fun `FriendshipEvent has correct properties`() {
        val event = FriendshipEvent(
            type = FriendshipEventType.CONVERSATION,
            npcId = "npc_1",
            description = "Had a conversation",
            xpChange = 25
        )

        assertEquals(FriendshipEventType.CONVERSATION, event.type)
        assertEquals("npc_1", event.npcId)
        assertEquals("Had a conversation", event.description)
        assertEquals(25, event.xpChange)
        assertTrue(event.id.isNotEmpty())
    }

    @Test
    fun `ConversationMemory has correct properties`() {
        val memory = ConversationMemory(
            npcId = "npc_1",
            dialogueId = "dialogue_1",
            dialogueTitle = "First Meeting",
            xpGained = 30,
            topicsDiscussed = listOf("greetings", "food"),
            choicesSummary = listOf("choice_1", "choice_2")
        )

        assertEquals("npc_1", memory.npcId)
        assertEquals("dialogue_1", memory.dialogueId)
        assertEquals("First Meeting", memory.dialogueTitle)
        assertEquals(30, memory.xpGained)
        assertEquals(listOf("greetings", "food"), memory.topicsDiscussed)
        assertEquals(listOf("choice_1", "choice_2"), memory.choicesSummary)
        assertTrue(memory.id.isNotEmpty())
    }

    @Test
    fun `GiftRecord has correct properties`() {
        val gift = GiftRecord(
            giftId = "gift_1",
            giftName = "Mooncake",
            friendshipXpBonus = 15
        )

        assertEquals("gift_1", gift.giftId)
        assertEquals("Mooncake", gift.giftName)
        assertEquals(15, gift.friendshipXpBonus)
    }

    @Test
    fun `FriendshipEventType has all required types`() {
        val types = FriendshipEventType.entries
        assertEquals(7, types.size)
        assertTrue(types.contains(FriendshipEventType.CONVERSATION))
        assertTrue(types.contains(FriendshipEventType.GIFT))
        assertTrue(types.contains(FriendshipEventType.QUEST_COMPLETE))
        assertTrue(types.contains(FriendshipEventType.LEVEL_UP))
        assertTrue(types.contains(FriendshipEventType.TOPIC_UNLOCK))
        assertTrue(types.contains(FriendshipEventType.FIRST_MEETING))
        assertTrue(types.contains(FriendshipEventType.DAILY_VISIT))
    }

    @Test
    fun `FriendshipLevel progression is correct`() {
        assertEquals(0, FriendshipLevel.STRANGER.xpThreshold)
        assertEquals(100, FriendshipLevel.VISITOR.xpThreshold)
        assertEquals(300, FriendshipLevel.FRIEND.xpThreshold)
        assertEquals(600, FriendshipLevel.CLOSE_FRIEND.xpThreshold)
        assertEquals(1000, FriendshipLevel.TRUSTED_FRIEND.xpThreshold)
        assertEquals(1500, FriendshipLevel.FAMILY.xpThreshold)
    }

    @Test
    fun `FriendshipLevel fromXp returns correct level`() {
        assertEquals(FriendshipLevel.STRANGER, FriendshipLevel.fromXp(0))
        assertEquals(FriendshipLevel.STRANGER, FriendshipLevel.fromXp(50))
        assertEquals(FriendshipLevel.VISITOR, FriendshipLevel.fromXp(100))
        assertEquals(FriendshipLevel.VISITOR, FriendshipLevel.fromXp(200))
        assertEquals(FriendshipLevel.FRIEND, FriendshipLevel.fromXp(300))
        assertEquals(FriendshipLevel.CLOSE_FRIEND, FriendshipLevel.fromXp(600))
        assertEquals(FriendshipLevel.TRUSTED_FRIEND, FriendshipLevel.fromXp(1000))
        assertEquals(FriendshipLevel.FAMILY, FriendshipLevel.fromXp(1500))
        assertEquals(FriendshipLevel.FAMILY, FriendshipLevel.fromXp(2000))
    }
}
