package com.sworddao.phoenix.feature.friendship

import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.npc.data.FriendshipLevel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FriendshipRepositoryTest {

    private lateinit var repository: MockFriendshipRepository

    @Before
    fun setup() {
        repository = MockFriendshipRepository()
    }

    @Test
    fun `initializeFriendship creates initial state`() = runTest {
        repository.initializeFriendship("npc_1")

        val state = repository.getFriendshipState("npc_1").firstOrNull()
        assertNotNull(state)
        assertEquals("npc_1", state?.npcId)
        assertEquals(0, state?.friendshipXp)
        assertEquals(FriendshipLevel.STRANGER, state?.friendshipLevel)
    }

    @Test
    fun `initializeFriendship does not overwrite existing state`() = runTest {
        repository.initializeFriendship("npc_1")
        repository.addFriendshipXp("npc_1", 50)

        repository.initializeFriendship("npc_1")

        val state = repository.getFriendshipState("npc_1").firstOrNull()
        assertEquals(50, state?.friendshipXp)
    }

    @Test
    fun `addFriendshipXp increases XP correctly`() = runTest {
        repository.initializeFriendship("npc_1")

        val state = repository.addFriendshipXp("npc_1", 50)

        assertNotNull(state)
        assertEquals(50, state?.friendshipXp)
        assertEquals(FriendshipLevel.STRANGER, state?.friendshipLevel)
    }

    @Test
    fun `addFriendshipXp triggers level up`() = runTest {
        repository.initializeFriendship("npc_1")

        val state = repository.addFriendshipXp("npc_1", 150)

        assertNotNull(state)
        assertEquals(150, state?.friendshipXp)
        assertEquals(FriendshipLevel.VISITOR, state?.friendshipLevel)
    }

    @Test
    fun `addFriendshipXp accumulates XP`() = runTest {
        repository.initializeFriendship("npc_1")

        repository.addFriendshipXp("npc_1", 50)
        repository.addFriendshipXp("npc_1", 75)

        val state = repository.getFriendshipState("npc_1").firstOrNull()
        assertEquals(125, state?.friendshipXp)
        assertEquals(FriendshipLevel.VISITOR, state?.friendshipLevel)
    }

    @Test
    fun `addFriendshipXp initializes NPC if not exists`() = runTest {
        val result = repository.addFriendshipXp("unknown_npc", 50)

        assertNotNull(result)
        assertEquals("unknown_npc", result?.npcId)
        assertEquals(50, result?.friendshipXp)
    }

    @Test
    fun `recordConversation increments conversation count`() = runTest {
        repository.initializeFriendship("npc_1")

        repository.recordConversation(
            npcId = "npc_1",
            dialogueId = "dialogue_1",
            dialogueTitle = "First Meeting",
            xpGained = 25
        )

        val state = repository.getFriendshipState("npc_1").firstOrNull()
        assertEquals(1, state?.totalConversations)
    }

    @Test
    fun `getConversationHistory returns memories in reverse chronological order`() = runTest {
        repository.initializeFriendship("npc_1")

        repository.recordConversation(
            npcId = "npc_1",
            dialogueId = "dialogue_1",
            dialogueTitle = "First Meeting",
            xpGained = 25
        )
        repository.recordConversation(
            npcId = "npc_1",
            dialogueId = "dialogue_2",
            dialogueTitle = "Second Meeting",
            xpGained = 30
        )

        val history = repository.getConversationHistory("npc_1").firstOrNull()
        assertNotNull(history)
        assertEquals(2, history?.size)
        assertEquals("Second Meeting", history?.first()?.dialogueTitle)
        assertEquals("First Meeting", history?.last()?.dialogueTitle)
    }

    @Test
    fun `getFriendshipEvents returns events`() = runTest {
        repository.initializeFriendship("npc_1")

        repository.recordConversation(
            npcId = "npc_1",
            dialogueId = "dialogue_1",
            dialogueTitle = "First Meeting",
            xpGained = 25
        )

        val events = repository.getFriendshipEvents("npc_1").firstOrNull()
        assertNotNull(events)
        assertTrue(events!!.isNotEmpty())
    }

    @Test
    fun `unlockTopic adds topic to state`() = runTest {
        repository.initializeFriendship("npc_1")

        repository.unlockTopic("npc_1", "food")

        val state = repository.getFriendshipState("npc_1").firstOrNull()
        assertTrue(state?.unlockedTopics?.contains("food") == true)
    }

    @Test
    fun `unlockTopic does not add duplicate topic`() = runTest {
        repository.initializeFriendship("npc_1")

        repository.unlockTopic("npc_1", "food")
        repository.unlockTopic("npc_1", "food")

        val state = repository.getFriendshipState("npc_1").firstOrNull()
        assertEquals(1, state?.unlockedTopics?.size)
    }

    @Test
    fun `getAllFriendshipStates returns all states`() = runTest {
        repository.initializeFriendship("npc_1")
        repository.initializeFriendship("npc_2")
        repository.initializeFriendship("npc_3")

        val states = repository.getAllFriendshipStates().firstOrNull()
        assertNotNull(states)
        assertEquals(3, states?.size)
    }

    @Test
    fun `friendship level progresses through all levels`() = runTest {
        repository.initializeFriendship("npc_1")

        var state = repository.addFriendshipXp("npc_1", 100)
        assertEquals(FriendshipLevel.VISITOR, state?.friendshipLevel)

        state = repository.addFriendshipXp("npc_1", 200)
        assertEquals(FriendshipLevel.FRIEND, state?.friendshipLevel)

        state = repository.addFriendshipXp("npc_1", 300)
        assertEquals(FriendshipLevel.CLOSE_FRIEND, state?.friendshipLevel)

        state = repository.addFriendshipXp("npc_1", 400)
        assertEquals(FriendshipLevel.TRUSTED_FRIEND, state?.friendshipLevel)

        state = repository.addFriendshipXp("npc_1", 500)
        assertEquals(FriendshipLevel.FAMILY, state?.friendshipLevel)
    }
}
