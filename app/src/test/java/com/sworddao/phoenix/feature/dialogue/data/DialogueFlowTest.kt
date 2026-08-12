package com.sworddao.phoenix.feature.dialogue.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueFlowTest {

    private fun buildDialogue(): Dialogue = Dialogue(
        id = "dlg_1",
        npcId = "npc_1",
        title = "Intro",
        description = "First chat",
        startNodeId = "start",
        nodes = listOf(
            DialogueNode(
                id = "start",
                type = DialogueNodeType.PLAYER_CHOOSES,
                speaker = Speaker.NPC,
                speakerName = "Grandma Mei",
                text = "Welcome!",
                pinyin = "huanying",
                choices = listOf(
                    DialogueChoice(
                        id = "c1",
                        text = "Say hello",
                        pinyin = "ni hao",
                        nextNodeId = "end",
                        actions = listOf(DialogueAction(ActionType.ADD_FRIENDSHIP_XP, value = "10"))
                    ),
                    DialogueChoice(
                        id = "c2",
                        text = "Ask about the village",
                        pinyin = "wen wen",
                        nextNodeId = "follow",
                        actions = listOf(DialogueAction(ActionType.UNLOCK_VOCABULARY, value = "Food"))
                    )
                )
            ),
            DialogueNode(
                id = "follow",
                type = DialogueNodeType.NPC_SPEAKS,
                speaker = Speaker.NPC,
                speakerName = "Grandma Mei",
                text = "This is a chain node",
                nextNodeId = "follow2"
            ),
            DialogueNode(
                id = "follow2",
                type = DialogueNodeType.CONVERSATION_END,
                speaker = Speaker.NPC,
                speakerName = "Grandma Mei",
                text = "Goodbye",
                actions = listOf(DialogueAction(ActionType.GIVE_ITEM, value = "tea"))
            ),
            DialogueNode(
                id = "end",
                type = DialogueNodeType.CONVERSATION_END,
                speaker = Speaker.NPC,
                speakerName = "Grandma Mei",
                text = "See you later"
            )
        )
    )

    private fun buildChainStartDialogue(): Dialogue = Dialogue(
        id = "chain",
        npcId = "npc_1",
        title = "Chain",
        description = "Auto-advance chain",
        startNodeId = "n1",
        nodes = listOf(
            DialogueNode(id = "n1", type = DialogueNodeType.NPC_SPEAKS, text = "one", nextNodeId = "n2"),
            DialogueNode(id = "n2", type = DialogueNodeType.NPC_SPEAKS, text = "two", nextNodeId = "n3"),
            DialogueNode(
                id = "n3",
                type = DialogueNodeType.PLAYER_CHOOSES,
                text = "choice",
                choices = listOf(DialogueChoice(id = "c1", text = "go", nextNodeId = "end"))
            ),
            DialogueNode(id = "end", type = DialogueNodeType.CONVERSATION_END, text = "done")
        )
    )

    private fun conversationState(
        dialogue: Dialogue,
        nodeId: String
    ): ConversationState = ConversationState(
        dialogueId = dialogue.id,
        npcId = dialogue.npcId,
        currentNodeId = nodeId,
        phase = ConversationPhase.IN_PROGRESS,
        history = emptyList(),
        availableChoices = emptyList(),
        completedActions = emptyList()
    )

    @Test
    fun `startConversation loads the start node with empty chain history`() {
        val (state, result) = DialogueFlow.startConversation(buildDialogue())

        assertTrue(result is DialogueResult.NodeLoaded)
        val nodeLoaded = result as DialogueResult.NodeLoaded
        assertEquals("start", nodeLoaded.node.id)
        assertEquals(1, nodeLoaded.history.size)
        assertEquals("Welcome!", nodeLoaded.history[0].text)
        assertEquals(listOf("c1", "c2"), nodeLoaded.choices.map { it.id })

        assertTrue(state != null)
        assertEquals("start", state?.currentNodeId)
        assertEquals(ConversationPhase.IN_PROGRESS, state?.phase)
        assertEquals(1, state?.history?.size)
        assertEquals(listOf("c1", "c2"), state?.availableChoices?.map { it.id })
    }

    @Test
    fun `startConversation auto-advances through NPC_SPEAKS chain`() {
        val (state, result) = DialogueFlow.startConversation(buildChainStartDialogue())

        assertTrue(result is DialogueResult.NodeLoaded)
        val nodeLoaded = result as DialogueResult.NodeLoaded
        assertEquals("n3", nodeLoaded.node.id)
        assertEquals(listOf("one", "two"), nodeLoaded.history.map { it.text })
        assertEquals(listOf("c1"), nodeLoaded.choices.map { it.id })

        assertEquals("n3", state?.currentNodeId)
        assertEquals(2, state?.history?.size)
    }

    @Test
    fun `startConversation with unknown start node returns error and no state`() {
        val dialogue = buildDialogue().copy(startNodeId = "missing")
        val (state, result) = DialogueFlow.startConversation(dialogue)

        assertTrue(result is DialogueResult.Error)
        assertEquals("Start node not found", (result as DialogueResult.Error).message)
        assertNull(state)
    }

    @Test
    fun `startConversation with terminal NPC_SPEAKS node returns single history entry`() {
        val dialogue = buildDialogue().copy(
            startNodeId = "solo",
            nodes = buildDialogue().nodes + DialogueNode(
                id = "solo",
                type = DialogueNodeType.NPC_SPEAKS,
                speaker = Speaker.NPC,
                speakerName = "Grandma Mei",
                text = "Hello alone"
            )
        )
        val (state, result) = DialogueFlow.startConversation(dialogue)

        assertTrue(result is DialogueResult.NodeLoaded)
        val nodeLoaded = result as DialogueResult.NodeLoaded
        assertEquals("solo", nodeLoaded.node.id)
        assertEquals(1, nodeLoaded.history.size)
        assertEquals("Hello alone", nodeLoaded.history[0].text)
        assertEquals("solo", state?.currentNodeId)
    }

    @Test
    fun `selectChoice with no active conversation returns error`() {
        val (state, result) = DialogueFlow.selectChoice(buildDialogue(), null, "c1")

        assertTrue(result is DialogueResult.Error)
        assertEquals("No active conversation", (result as DialogueResult.Error).message)
        assertNull(state)
    }

    @Test
    fun `selectChoice with unknown choice returns error`() {
        val (startState, _) = DialogueFlow.startConversation(buildDialogue())
        val (state, result) = DialogueFlow.selectChoice(buildDialogue(), startState, "nope")

        assertTrue(result is DialogueResult.Error)
        assertEquals("Choice not found", (result as DialogueResult.Error).message)
        assertEquals(startState, state)
    }

    @Test
    fun `selectChoice to conversation end returns ConversationEnded`() {
        val (startState, _) = DialogueFlow.startConversation(buildDialogue())
        val (state, result) = DialogueFlow.selectChoice(buildDialogue(), startState, "c1")

        assertTrue(result is DialogueResult.ConversationEnded)
        val ended = result as DialogueResult.ConversationEnded
        assertEquals(listOf(ActionType.ADD_FRIENDSHIP_XP), ended.actions.map { it.type })
        assertEquals(3, ended.history.size)
        assertEquals(Speaker.PLAYER, ended.history[1].speaker)
        assertEquals(Speaker.NPC, ended.history[2].speaker)

        assertTrue(state != null)
        assertEquals("end", state?.currentNodeId)
        assertEquals(ConversationPhase.COMPLETED, state?.phase)
        assertTrue(state?.availableChoices?.isEmpty() == true)
    }

    @Test
    fun `selectChoice to non-terminal node returns NodeLoaded and accumulates history`() {
        val (startState, _) = DialogueFlow.startConversation(buildDialogue())
        val (state, result) = DialogueFlow.selectChoice(buildDialogue(), startState, "c2")

        assertTrue(result is DialogueResult.NodeLoaded)
        val nodeLoaded = result as DialogueResult.NodeLoaded
        assertEquals("follow", nodeLoaded.node.id)
        assertEquals(3, nodeLoaded.history.size)
        assertTrue(nodeLoaded.choices.isEmpty())

        assertEquals("follow", state?.currentNodeId)
        assertEquals(ConversationPhase.IN_PROGRESS, state?.phase)
        assertEquals(listOf(ActionType.UNLOCK_VOCABULARY), state?.completedActions?.map { it.type })
    }

    @Test
    fun `selectChoice with state on missing node returns error`() {
        val dialogue = buildDialogue()
        val strayState = conversationState(dialogue, "missing")
        val (state, result) = DialogueFlow.selectChoice(dialogue, strayState, "c1")

        assertTrue(result is DialogueResult.Error)
        assertEquals("Current node not found", (result as DialogueResult.Error).message)
        assertEquals(strayState, state)
    }

    @Test
    fun `selectChoice with choice pointing to missing next node returns error`() {
        val dialogue = buildDialogue()
        val choiceToMissing = DialogueNode(
            id = "branch",
            type = DialogueNodeType.PLAYER_CHOOSES,
            text = "branch",
            choices = listOf(DialogueChoice(id = "cB", text = "go", nextNodeId = "does_not_exist"))
        )
        val branched = dialogue.copy(
            nodes = dialogue.nodes + choiceToMissing,
            startNodeId = "branch"
        )
        val (startState, _) = DialogueFlow.startConversation(branched)
        val (state, result) = DialogueFlow.selectChoice(branched, startState, "cB")

        assertTrue(result is DialogueResult.Error)
        assertEquals("Next node not found", (result as DialogueResult.Error).message)
        assertEquals(startState, state)
    }

    @Test
    fun `advanceDialogue with no active conversation returns error`() {
        val (state, result) = DialogueFlow.advanceDialogue(buildDialogue(), null)

        assertTrue(result is DialogueResult.Error)
        assertEquals("No active conversation", (result as DialogueResult.Error).message)
        assertNull(state)
    }

    @Test
    fun `advanceDialogue follows chain to conversation end`() {
        val dialogue = buildDialogue()
        val (startState, _) = DialogueFlow.startConversation(dialogue)
        val (followState, _) = DialogueFlow.selectChoice(dialogue, startState, "c2")

        val (state, result) = DialogueFlow.advanceDialogue(dialogue, followState)

        assertTrue(result is DialogueResult.ConversationEnded)
        val ended = result as DialogueResult.ConversationEnded
        assertEquals(listOf(ActionType.UNLOCK_VOCABULARY, ActionType.GIVE_ITEM), ended.actions.map { it.type })

        assertTrue(state != null)
        assertEquals("follow2", state?.currentNodeId)
        assertEquals(ConversationPhase.COMPLETED, state?.phase)
        assertEquals(listOf(ActionType.UNLOCK_VOCABULARY, ActionType.GIVE_ITEM), state?.completedActions?.map { it.type })
    }

    @Test
    fun `advanceDialogue on node with no next returns error`() {
        val dialogue = buildDialogue().copy(
            startNodeId = "solo",
            nodes = buildDialogue().nodes + DialogueNode(
                id = "solo",
                type = DialogueNodeType.NPC_SPEAKS,
                text = "dead end"
            )
        )
        val (startState, _) = DialogueFlow.startConversation(dialogue)
        val (state, result) = DialogueFlow.advanceDialogue(dialogue, startState)

        assertTrue(result is DialogueResult.Error)
        assertEquals("No next node defined", (result as DialogueResult.Error).message)
        assertEquals(startState, state)
    }

    @Test
    fun `advanceDialogue with state on missing node returns error`() {
        val dialogue = buildDialogue()
        val strayState = conversationState(dialogue, "missing")
        val (state, result) = DialogueFlow.advanceDialogue(dialogue, strayState)

        assertTrue(result is DialogueResult.Error)
        assertEquals("Current node not found", (result as DialogueResult.Error).message)
        assertEquals(strayState, state)
    }

    @Test
    fun `advanceDialogue to missing next node returns error`() {
        val dialogue = buildDialogue().copy(
            startNodeId = "dangling",
            nodes = buildDialogue().nodes + DialogueNode(
                id = "dangling",
                type = DialogueNodeType.NPC_SPEAKS,
                text = "dangling",
                nextNodeId = "ghost"
            )
        )
        val (startState, _) = DialogueFlow.startConversation(dialogue)
        val (state, result) = DialogueFlow.advanceDialogue(dialogue, startState)

        assertTrue(result is DialogueResult.Error)
        assertEquals("Next node not found", (result as DialogueResult.Error).message)
        assertEquals(startState, state)
    }
}
