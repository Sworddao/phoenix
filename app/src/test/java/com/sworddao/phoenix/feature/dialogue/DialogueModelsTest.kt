package com.sworddao.phoenix.feature.dialogue

import com.sworddao.phoenix.feature.dialogue.data.ActionType
import com.sworddao.phoenix.feature.dialogue.data.ConditionType
import com.sworddao.phoenix.feature.dialogue.data.ConversationPhase
import com.sworddao.phoenix.feature.dialogue.data.ConversationState
import com.sworddao.phoenix.feature.dialogue.data.Dialogue
import com.sworddao.phoenix.feature.dialogue.data.DialogueAction
import com.sworddao.phoenix.feature.dialogue.data.DialogueChoice
import com.sworddao.phoenix.feature.dialogue.data.DialogueCondition
import com.sworddao.phoenix.feature.dialogue.data.DialogueHistoryEntry
import com.sworddao.phoenix.feature.dialogue.data.DialogueNode
import com.sworddao.phoenix.feature.dialogue.data.DialogueNodeType
import com.sworddao.phoenix.feature.dialogue.data.MockDialogueRepository
import com.sworddao.phoenix.feature.dialogue.data.Speaker
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DialogueModelsTest {

    private lateinit var repository: MockDialogueRepository

    @Before
    fun setup() {
        repository = MockDialogueRepository()
    }

    @Test
    fun `DialogueNode has correct properties`() {
        val node = DialogueNode(
            id = "test_node",
            type = DialogueNodeType.NPC_SPEAKS,
            speaker = Speaker.NPC,
            speakerName = "Test NPC",
            text = "Hello!",
            pinyin = "Nǐ hǎo!",
            nextNodeId = "next_node"
        )

        assertEquals("test_node", node.id)
        assertEquals(DialogueNodeType.NPC_SPEAKS, node.type)
        assertEquals(Speaker.NPC, node.speaker)
        assertEquals("Test NPC", node.speakerName)
        assertEquals("Hello!", node.text)
        assertEquals("Nǐ hǎo!", node.pinyin)
        assertEquals("next_node", node.nextNodeId)
        assertTrue(node.choices.isEmpty())
    }

    @Test
    fun `DialogueChoice has correct properties`() {
        val choice = DialogueChoice(
            id = "choice_1",
            text = "Response text",
            pinyin = "Response pinyin",
            nextNodeId = "next_node",
            conditions = emptyList(),
            actions = listOf(
                DialogueAction(
                    type = ActionType.ADD_FRIENDSHIP_XP,
                    targetId = "npc_1",
                    value = "25"
                )
            )
        )

        assertEquals("choice_1", choice.id)
        assertEquals("Response text", choice.text)
        assertEquals("Response pinyin", choice.pinyin)
        assertEquals("next_node", choice.nextNodeId)
        assertTrue(choice.conditions.isEmpty())
        assertEquals(1, choice.actions.size)
        assertEquals(ActionType.ADD_FRIENDSHIP_XP, choice.actions[0].type)
    }

    @Test
    fun `DialogueCondition has correct properties`() {
        val condition = DialogueCondition(
            type = ConditionType.FRIENDSHIP_LEVEL,
            targetId = "npc_1",
            value = "3"
        )

        assertEquals(ConditionType.FRIENDSHIP_LEVEL, condition.type)
        assertEquals("npc_1", condition.targetId)
        assertEquals("3", condition.value)
    }

    @Test
    fun `DialogueAction has correct properties`() {
        val action = DialogueAction(
            type = ActionType.UNLOCK_VOCABULARY,
            targetId = "vocab_1",
            value = "word1,word2"
        )

        assertEquals(ActionType.UNLOCK_VOCABULARY, action.type)
        assertEquals("vocab_1", action.targetId)
        assertEquals("word1,word2", action.value)
    }

    @Test
    fun `DialogueHistoryEntry has correct properties`() {
        val entry = DialogueHistoryEntry(
            speaker = Speaker.NPC,
            speakerName = "Test NPC",
            text = "Hello!",
            pinyin = "Nǐ hǎo!",
            hanzi = "你好"
        )

        assertEquals(Speaker.NPC, entry.speaker)
        assertEquals("Test NPC", entry.speakerName)
        assertEquals("Hello!", entry.text)
        assertEquals("Nǐ hǎo!", entry.pinyin)
        assertEquals("你好", entry.hanzi)
    }

    @Test
    fun `ConversationState has correct properties`() {
        val state = ConversationState(
            dialogueId = "dialogue_1",
            npcId = "npc_1",
            currentNodeId = "node_1",
            phase = ConversationPhase.IN_PROGRESS,
            history = emptyList(),
            availableChoices = emptyList(),
            completedActions = emptyList()
        )

        assertEquals("dialogue_1", state.dialogueId)
        assertEquals("npc_1", state.npcId)
        assertEquals("node_1", state.currentNodeId)
        assertEquals(ConversationPhase.IN_PROGRESS, state.phase)
        assertTrue(state.history.isEmpty())
        assertTrue(state.availableChoices.isEmpty())
        assertTrue(state.completedActions.isEmpty())
    }

    @Test
    fun `Dialogue finds node by ID`() {
        val node1 = DialogueNode(
            id = "node_1",
            type = DialogueNodeType.NPC_SPEAKS,
            text = "First node"
        )
        val node2 = DialogueNode(
            id = "node_2",
            type = DialogueNodeType.PLAYER_CHOOSES,
            text = "Second node"
        )

        val dialogue = Dialogue(
            id = "dialogue_1",
            npcId = "npc_1",
            title = "Test Dialogue",
            description = "A test dialogue",
            startNodeId = "node_1",
            nodes = listOf(node1, node2)
        )

        assertEquals(node1, dialogue.getNodeById("node_1"))
        assertEquals(node2, dialogue.getNodeById("node_2"))
        assertNull(dialogue.getNodeById("node_3"))
    }

    @Test
    fun `MockDialogueRepository loads Grandma Mei dialogue`() = runTest {
        val dialogue = repository.getDialogueByNpcId("grandma_mei")
            .firstOrNull()

        assertNotNull(dialogue)
        assertEquals("grandma_mei_greeting", dialogue?.id)
        assertEquals("grandma_mei", dialogue?.npcId)
        assertEquals("Meeting Grandma Mei", dialogue?.title)
        assertTrue(dialogue?.nodes?.isNotEmpty() == true)
    }

    @Test
    fun `MockDialogueRepository starts conversation`() = runTest {
        val result = repository.startConversation("grandma_mei_greeting")

        assertTrue(result is com.sworddao.phoenix.feature.dialogue.data.DialogueResult.NodeLoaded)
        val nodeLoaded = result as com.sworddao.phoenix.feature.dialogue.data.DialogueResult.NodeLoaded
        assertEquals("player_respond_1", nodeLoaded.node.id)
        assertEquals(1, nodeLoaded.history.size)
        assertEquals(Speaker.NPC, nodeLoaded.history[0].speaker)
        assertEquals(3, nodeLoaded.choices.size)
    }

    @Test
    fun `MockDialogueRepository selects choice`() = runTest {
        repository.startConversation("grandma_mei_greeting")

        val result = repository.selectChoice("grandma_mei_greeting", "choice_greeting")

        assertTrue(result is com.sworddao.phoenix.feature.dialogue.data.DialogueResult.NodeLoaded)
        val nodeLoaded = result as com.sworddao.phoenix.feature.dialogue.data.DialogueResult.NodeLoaded
        assertEquals("mei_happy", nodeLoaded.node.id)
        assertEquals(3, nodeLoaded.history.size)
    }

    @Test
    fun `MockDialogueRepository completes conversation`() = runTest {
        repository.startConversation("grandma_mei_greeting")
        repository.selectChoice("grandma_mei_greeting", "choice_greeting")
        repository.advanceDialogue("grandma_mei_greeting")
        repository.selectChoice("grandma_mei_greeting", "choice_learn")
        val result = repository.advanceDialogue("grandma_mei_greeting")

        assertTrue(result is com.sworddao.phoenix.feature.dialogue.data.DialogueResult.ConversationEnded)
    }
}
