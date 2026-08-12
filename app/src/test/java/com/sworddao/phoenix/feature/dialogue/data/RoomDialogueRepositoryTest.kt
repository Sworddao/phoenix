package com.sworddao.phoenix.feature.dialogue.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
import com.sworddao.phoenix.data.seed.DialogueSeedData
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
class RoomDialogueRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomDialogueRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        repository = createRepository()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createRepository(): RoomDialogueRepository = RoomDialogueRepository(database.dialogueDao())

    @Test
    fun `first access seeds dialogues from seed data`() = runBlocking {
        val dialogues = repository.getAllDialogues().first()

        assertEquals(DialogueSeedData.loadDialogues().size, dialogues.size)
        assertEquals("grandma_mei_greeting", dialogues.single().id)
        assertTrue(dialogues.single().nodes.isNotEmpty())
    }

    @Test
    fun `getDialogueByNpcId returns the grandma mei dialogue`() = runBlocking {
        val dialogue = repository.getDialogueByNpcId("grandma_mei").first()

        assertNotNull(dialogue)
        assertEquals("grandma_mei_greeting", dialogue?.id)
        assertEquals("Meeting Grandma Mei", dialogue?.title)
        assertEquals("grandma_mei", dialogue?.npcId)
    }

    @Test
    fun `getDialogueByNpcId returns null for npc without dialogue`() = runBlocking {
        val dialogue = repository.getDialogueByNpcId("restaurant_owner_lin").first()

        assertEquals(null, dialogue)
    }

    @Test
    fun `getDialogueByNpcId returns null for unknown npc`() = runBlocking {
        val dialogue = repository.getDialogueByNpcId("unknown_npc").first()

        assertEquals(null, dialogue)
    }

    @Test
    fun `startConversation returns the first player choice node`() = runBlocking {
        val result = repository.startConversation("grandma_mei_greeting")

        assertTrue(result is DialogueResult.NodeLoaded)
        val nodeLoaded = result as DialogueResult.NodeLoaded
        assertEquals("player_respond_1", nodeLoaded.node.id)
        assertEquals(1, nodeLoaded.history.size)
        assertEquals(Speaker.NPC, nodeLoaded.history[0].speaker)
        assertEquals(3, nodeLoaded.choices.size)
    }

    @Test
    fun `startConversation returns error for unknown dialogue`() = runBlocking {
        val result = repository.startConversation("unknown_dialogue")

        assertTrue(result is DialogueResult.Error)
        assertEquals("Dialogue not found", (result as DialogueResult.Error).message)
    }

    @Test
    fun `selectChoice advances to the chosen branch`() = runBlocking {
        repository.startConversation("grandma_mei_greeting")

        val result = repository.selectChoice("grandma_mei_greeting", "choice_greeting")

        assertTrue(result is DialogueResult.NodeLoaded)
        val nodeLoaded = result as DialogueResult.NodeLoaded
        assertEquals("mei_happy", nodeLoaded.node.id)
        assertEquals(3, nodeLoaded.history.size)
    }

    @Test
    fun `selectChoice returns error without an active conversation`() = runBlocking {
        val result = repository.selectChoice("grandma_mei_greeting", "choice_greeting")

        assertTrue(result is DialogueResult.Error)
        assertEquals("No active conversation", (result as DialogueResult.Error).message)
    }

    @Test
    fun `selectChoice returns error for unknown choice`() = runBlocking {
        repository.startConversation("grandma_mei_greeting")

        val result = repository.selectChoice("grandma_mei_greeting", "unknown_choice")

        assertTrue(result is DialogueResult.Error)
        assertEquals("Choice not found", (result as DialogueResult.Error).message)
    }

    @Test
    fun `advanceDialogue moves through an npc speaks node`() = runBlocking {
        repository.startConversation("grandma_mei_greeting")

        val result = repository.advanceDialogue("grandma_mei_greeting")

        assertTrue(result is DialogueResult.Error)
        assertEquals("No next node defined", (result as DialogueResult.Error).message)
    }

    @Test
    fun `advanceDialogue returns error without an active conversation`() = runBlocking {
        val result = repository.advanceDialogue("grandma_mei_greeting")

        assertTrue(result is DialogueResult.Error)
        assertEquals("No active conversation", (result as DialogueResult.Error).message)
    }

    @Test
    fun `full conversation flow completes with conversation ended`() = runBlocking {
        repository.startConversation("grandma_mei_greeting")
        repository.selectChoice("grandma_mei_greeting", "choice_greeting")
        repository.advanceDialogue("grandma_mei_greeting")
        repository.selectChoice("grandma_mei_greeting", "choice_learn")

        val result = repository.advanceDialogue("grandma_mei_greeting")

        assertTrue(result is DialogueResult.ConversationEnded)
        val ended = result as DialogueResult.ConversationEnded
        assertTrue(ended.actions.isNotEmpty())
        assertTrue(ended.history.isNotEmpty())
    }

    @Test
    fun `dialogue catalog persists across repository instances`() = runBlocking {
        repository.startConversation("grandma_mei_greeting")

        val freshRepository = createRepository()
        val dialogue = freshRepository.getDialogueByNpcId("grandma_mei").first()
        assertNotNull(dialogue)
        assertEquals("grandma_mei_greeting", dialogue?.id)

        val result = freshRepository.startConversation("grandma_mei_greeting")
        assertTrue(result is DialogueResult.NodeLoaded)
    }

    @Test
    fun `dialogue entity round trip preserves nodes and actions`() {
        val dialogue = DialogueSeedData.loadDialogues().single()

        val entity = dialogue.toEntity()
        val restored = entity.toDomain()

        assertEquals(dialogue.id, restored.id)
        assertEquals(dialogue.npcId, restored.npcId)
        assertEquals(dialogue.startNodeId, restored.startNodeId)
        assertEquals(dialogue.requiredFriendshipLevel, restored.requiredFriendshipLevel)
        assertEquals(dialogue.nodes.size, restored.nodes.size)
        assertEquals(dialogue.getNodeById("player_respond_1")?.choices?.size, restored.getNodeById("player_respond_1")?.choices?.size)
        assertEquals(
            dialogue.getNodeById("mei_teach")?.actions?.map { it.type },
            restored.getNodeById("mei_teach")?.actions?.map { it.type }
        )
    }
}
