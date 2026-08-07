package com.sworddao.phoenix.feature.friendship

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
import com.sworddao.phoenix.feature.friendship.data.RoomFriendshipRepository
import com.sworddao.phoenix.feature.npc.data.FriendshipLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomFriendshipRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomFriendshipRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        repository = RoomFriendshipRepository(database.friendshipDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `initializeFriendship creates initial state`() = runBlocking {
        repository.initializeFriendship("npc_1")

        val state = repository.getFriendshipState("npc_1").first()
        assertNotNull(state)
        assertEquals("npc_1", state?.npcId)
        assertEquals(0, state?.friendshipXp)
        assertEquals(FriendshipLevel.STRANGER, state?.friendshipLevel)
    }

    @Test
    fun `initializeFriendship does not overwrite existing state`() = runBlocking {
        repository.initializeFriendship("npc_1")
        repository.addFriendshipXp("npc_1", 50)

        repository.initializeFriendship("npc_1")

        val state = repository.getFriendshipState("npc_1").first()
        assertEquals(50, state?.friendshipXp)
    }

    @Test
    fun `addFriendshipXp increases XP correctly`() = runBlocking {
        repository.initializeFriendship("npc_1")

        val state = repository.addFriendshipXp("npc_1", 50)

        assertNotNull(state)
        assertEquals(50, state?.friendshipXp)
        assertEquals(FriendshipLevel.STRANGER, state?.friendshipLevel)
    }

    @Test
    fun `addFriendshipXp triggers level up`() = runBlocking {
        repository.initializeFriendship("npc_1")

        val state = repository.addFriendshipXp("npc_1", 150)

        assertNotNull(state)
        assertEquals(150, state?.friendshipXp)
        assertEquals(FriendshipLevel.VISITOR, state?.friendshipLevel)
    }

    @Test
    fun `addFriendshipXp accumulates XP`() = runBlocking {
        repository.initializeFriendship("npc_1")

        repository.addFriendshipXp("npc_1", 50)
        repository.addFriendshipXp("npc_1", 75)

        val state = repository.getFriendshipState("npc_1").first()
        assertEquals(125, state?.friendshipXp)
        assertEquals(FriendshipLevel.VISITOR, state?.friendshipLevel)
    }

    @Test
    fun `addFriendshipXp initializes NPC if not exists`() = runBlocking {
        val result = repository.addFriendshipXp("unknown_npc", 50)

        assertNotNull(result)
        assertEquals("unknown_npc", result?.npcId)
        assertEquals(50, result?.friendshipXp)
    }

    @Test
    fun `recordConversation increments conversation count and stores memory`() = runBlocking {
        repository.initializeFriendship("npc_1")

        repository.recordConversation(
            npcId = "npc_1",
            dialogueId = "dialogue_1",
            dialogueTitle = "First Meeting",
            xpGained = 25,
        )

        val state = repository.getFriendshipState("npc_1").first()
        assertEquals(1, state?.totalConversations)

        val history = repository.getConversationHistory("npc_1").first()
        assertTrue(history.isNotEmpty())
        assertEquals("First Meeting", history.first().dialogueTitle)
    }

    @Test
    fun `getConversationHistory returns memories in reverse chronological order`() = runBlocking {
        repository.initializeFriendship("npc_1")

        repository.recordConversation(
            npcId = "npc_1",
            dialogueId = "dialogue_1",
            dialogueTitle = "First Meeting",
            xpGained = 25,
        )
        repository.recordConversation(
            npcId = "npc_1",
            dialogueId = "dialogue_2",
            dialogueTitle = "Second Meeting",
            xpGained = 30,
        )

        val history = repository.getConversationHistory("npc_1").first()
        assertNotNull(history)
        assertEquals(2, history.size)
        assertEquals("Second Meeting", history.first().dialogueTitle)
        assertEquals("First Meeting", history.last().dialogueTitle)
    }

    @Test
    fun `getFriendshipEvents returns events`() = runBlocking {
        repository.initializeFriendship("npc_1")

        repository.recordConversation(
            npcId = "npc_1",
            dialogueId = "dialogue_1",
            dialogueTitle = "First Meeting",
            xpGained = 25,
        )

        val events = repository.getFriendshipEvents("npc_1").first()
        assertNotNull(events)
        assertTrue(events.isNotEmpty())
    }

    @Test
    fun `unlockTopic adds topic to state`() = runBlocking {
        repository.initializeFriendship("npc_1")

        repository.unlockTopic("npc_1", "food")

        val state = repository.getFriendshipState("npc_1").first()
        assertTrue(state?.unlockedTopics?.contains("food") == true)
    }

    @Test
    fun `unlockTopic does not add duplicate topic`() = runBlocking {
        repository.initializeFriendship("npc_1")

        repository.unlockTopic("npc_1", "food")
        repository.unlockTopic("npc_1", "food")

        val state = repository.getFriendshipState("npc_1").first()
        assertEquals(1, state?.unlockedTopics?.size)
    }

    @Test
    fun `getAllFriendshipStates returns all states`() = runBlocking {
        repository.initializeFriendship("npc_1")
        repository.initializeFriendship("npc_2")
        repository.initializeFriendship("npc_3")

        val states = repository.getAllFriendshipStates().first()
        assertNotNull(states)
        assertEquals(3, states.size)
    }

    @Test
    fun `friendship level progresses through all levels`() = runBlocking {
        repository.initializeFriendship("npc_1")

        var state = repository.addFriendshipXp("npc_1", 100)
        assertEquals(FriendshipLevel.VISITOR, state?.friendshipLevel)

        state = repository.addFriendshipXp("npc_1", 200)
        assertEquals(FriendshipLevel.FRIEND, state?.friendshipLevel)

        state = repository.addFriendshipXp("npc_1", 300)
        assertEquals(FriendshipLevel.CLOSE_FRIEND, state?.friendshipLevel)

        state = repository.addFriendshipXp("npc_1", 400)
        assertEquals(FriendshipLevel.TRUSTED_FRIEND, state?.friendshipLevel)
    }
}
